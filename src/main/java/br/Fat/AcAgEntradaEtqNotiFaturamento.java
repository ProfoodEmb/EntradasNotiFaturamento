package br.Fat;

import Classes.ProdutoEntrada;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;
import utils.WebHookService;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class    AcAgEntradaEtqNotiFaturamento implements ScheduledAction {
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onTime(ScheduledActionContext scheduledActionContext) {
        // Obtém a data de hoje
        LocalDate hoje = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String sqlWhere;
        String dataPeriodo; // Para exibir no assunto do e-mail ou onde for necessário

        if (hoje.getDayOfWeek() == DayOfWeek.MONDAY) {
            // Se hoje é segunda-feira, busca entradas de sexta, sábado e domingo
            sqlWhere = " AND CAB.DTNEG BETWEEN TRUNC(SYSDATE) - 3 AND TRUNC(SYSDATE) - 1";
            // Define o período para exibição: de sexta a domingo
            LocalDate dataInicio = hoje.minusDays(3);
            LocalDate dataFim = hoje.minusDays(1);
            dataPeriodo = dataInicio.format(fmt) + " a " + dataFim.format(fmt);
        } else {
            // Se não for segunda, busca apenas as entradas de ontem
            sqlWhere = " AND CAB.DTNEG = TRUNC(SYSDATE) - 1";
            LocalDate dataConsulta = hoje.minusDays(1);
            dataPeriodo = dataConsulta.format(fmt);
        }

        try {
            EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
            JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
            jdbc.openSession();

            // Exemplo de montagem da query, concatenando a condição de data:
            String sql =
                    "SELECT\n" +
                            "    CAB.nunota AS NOTA_ENTRADA, \n" +
                            "    PRO.codprod AS CODIGO_PRODUTO,\n" +
                            "    PRO.descrprod AS DESCRICAO_PRODUTO,\n" +
                            "    SUM(CAST(ITE.qtdneg AS INT)) AS QUANTIDADE_ENTRADA, \n" +
                            "    ITE.codvol AS VOLUME_ENTRADA,\n" +
                            "    VENDA_PROD.nunota AS NOTA_VENDA, \n" +
                            "    COALESCE(VENDA_PROD.qtdneg, 0) AS QUANTIDADE_PEDIDO_VENDA, \n" +
                            "    VENDA_PROD.codvol AS VOLUME_VENDA,\n" +
                            "    TO_CHAR(VENDA_PROD.dtneg, 'dd/mm/yyyy') AS DATA_PEDIDO, \n" +
                            "    VEN_VENDA.apelido AS VENDEDOR, \n" +
                            "    VEN_VENDA.email AS EMAIL,\n" +
                            "    MAT.nomeparc AS MATRIZ,\n" +
                            "    TPO.codTipOper AS CODTOP,\n" +
                            "    TPO.descrOper AS DESCTOP\n" +
                            "FROM tgfcab CAB\n" +
                            "INNER JOIN tgfite ITE ON CAB.nunota = ITE.nunota\n" +
                            "INNER JOIN tgfpro PRO ON ITE.codprod = PRO.codprod\n" +
                            "INNER JOIN tgfpar PAR_PROD ON PAR_PROD.codparc = PRO.AD_CODPARC\n" +
                            "INNER JOIN tgfpar MAT ON PAR_PROD.codparcmatriz = MAT.codparc\n" +
                            "LEFT JOIN (\n" +
                            "    SELECT V.nunota, V.codvend, V.dtneg, I.codprod, I.qtdneg, I.codvol\n" +
                            "    FROM tgfcab V\n" +
                            "    INNER JOIN tgfite I ON V.nunota = I.nunota\n" +
                            "    WHERE V.tipmov = 'P'\n" +
                            "      AND V.pendente = 'S'\n" +
                            "      AND I.pendente = 'S'\n" +
                            ") VENDA_PROD ON VENDA_PROD.codprod = ITE.codprod\n" +
                            "LEFT JOIN tgfven VEN_VENDA ON VENDA_PROD.codvend = VEN_VENDA.codvend\n" +
                            "LEFT JOIN tgftop TPO ON CAB.codtipoper = TPO.codtipoper AND CAB.dhtipoper = TPO.dhalter\n" +
                            "WHERE CAB.TIPMOV = 'C'\n" +
                            "    AND CAB.CODTIPOPER IN (212, 259)\n" +
                            "    AND PRO.CODPROD NOT IN (5, 8)\n" +
                            sqlWhere + "\n" +  // ← mantém aqui a montagem dinâmica do período (como estava antes!)
                            "GROUP BY\n" +
                            "    TPO.descrOper,\n" +
                            "    TPO.codTipOper,\n" +
                            "    CAB.nunota,\n" +
                            "    PRO.codprod,\n" +
                            "    PRO.descrprod,\n" +
                            "    VENDA_PROD.nunota,\n" +
                            "    VENDA_PROD.qtdneg,\n" +
                            "    VENDA_PROD.dtneg,\n" +
                            "    VEN_VENDA.apelido,\n" +
                            "    VEN_VENDA.email,\n" +
                            "    MAT.nomeparc,\n" +
                            "    ITE.codvol,\n" +
                            "    VENDA_PROD.codvol\n" +
                            "ORDER BY\n" +
                            "    VENDA_PROD.dtneg,\n" +
                            "    CAB.nunota,\n" +
                            "    VENDA_PROD.nunota";

            List<ProdutoEntrada> produtosEntrada = new ArrayList<>();

            try (PreparedStatement stmt = jdbc.getPreparedStatement(sql);

                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    // Cria um objeto do tipo ProdutoEntrada
                    ProdutoEntrada produtoEntrada = new ProdutoEntrada();

                    //================================================================================
                    Integer codTop = rs.getObject("CODTOP", Integer.class);
                    if (codTop == null) {
                        // Se a coluna for nula no banco
                        codTop = 0;
                    }
                    produtoEntrada.setCodTop(codTop);

                    //================================================================================

                    //================================================================================
                    produtoEntrada.setDescrTop(//DESCTOP
                            rs.getString("DESCTOP") != null
                                    ? rs.getString("DESCTOP")
                                    : "0"
                    );

                    //================================================================================

                    //================================================================================
                    Integer notaEntrada = rs.getObject("NOTA_ENTRADA", Integer.class);
                    if (notaEntrada == null) {
                        // Se a coluna for nula no banco
                        notaEntrada = 0;
                    }
                    produtoEntrada.setNotaEntrada(notaEntrada);

                    //================================================================================

                    Integer codigoProduto = rs.getObject("CODIGO_PRODUTO", Integer.class);
                    if (codigoProduto == null) {
                        // Se a coluna for nula no banco
                        codigoProduto = 0;
                    }

                    produtoEntrada.setCodigoProduto(codigoProduto);

                    //================================================================================

                    produtoEntrada.setDescricaoProduto(//DESCRICAO_PRODUTO
                            rs.getString("DESCRICAO_PRODUTO") != null
                                    ? rs.getString("DESCRICAO_PRODUTO")
                                    : "0"
                    );

                    //================================================================================

                    Integer quantidadeEntrada = rs.getObject("QUANTIDADE_ENTRADA", Integer.class);
                    if (quantidadeEntrada == null) {
                        // Se a coluna for nula no banco
                        quantidadeEntrada = 0;
                    }
                    produtoEntrada.setQuantidadeEntrada(quantidadeEntrada);

                    //================================================================================

                    Integer notaVenda = rs.getObject("NOTA_VENDA", Integer.class);
                    if (notaVenda == null) {
                        // Se a coluna for nula no banco
                        notaVenda = 0;
                    }
                    produtoEntrada.setNotaVenda(notaVenda);

                    //================================================================================

                    Integer quantidadePedidoVenda = rs.getObject("QUANTIDADE_PEDIDO_VENDA", Integer.class);
                    if (quantidadePedidoVenda == null) {
                        // Se a coluna for nula no banco
                        quantidadePedidoVenda = 0;
                    }
                    produtoEntrada.setQuantidadePedidoVenda(quantidadePedidoVenda);

                    //================================================================================

                    produtoEntrada.setDataPedido(//DATA_PEDIDO
                            rs.getString("DATA_PEDIDO") != null
                                    ? rs.getString("DATA_PEDIDO")
                                    : "0"
                    );

                    //================================================================================

                    produtoEntrada.setVendedor(//VENDEDOR
                            rs.getString("VENDEDOR") != null
                                    ? rs.getString("VENDEDOR")
                                    : "0"
                    );

                    //================================================================================

                    produtoEntrada.setEmail(//EMAIL
                            rs.getString("EMAIL") != null
                                    ? rs.getString("EMAIL")
                                    : "0"
                    );

                    //================================================================================

                    produtoEntrada.setVolumeProduto(//VOLUME
                            rs.getString("VOLUME_VENDA") != null
                                    ? rs.getString("VOLUME_VENDA")
                                    : ""
                    );

                    //================================================================================

                    produtoEntrada.setVolumeRecebido(//VOLUME_ENTRADA
                            rs.getString("VOLUME_ENTRADA") != null
                                    ? rs.getString("VOLUME_ENTRADA")
                                    : ""
                    );

                    //================================================================================

                    produtoEntrada.setMatriz(//MATRIZ
                            rs.getString("MATRIZ") != null
                                    ? rs.getString("MATRIZ")
                                    : ""
                    );

                    //================================================================================

                    // Adiciona o objeto 'pedido' na lista
                    produtosEntrada.add(produtoEntrada);
                }

                //Se o dia atual for diferente de sabado e domingo
                // if(hoje.getDayOfWeek() != DayOfWeek.SATURDAY && hoje.getDayOfWeek() != DayOfWeek.SUNDAY){
                // String emailHtml = gerarHtmlPedidosSemInline(gerarPedidosFalsos());
                String emailHtml = gerarHtmlPedidosSemInline(produtosEntrada);

                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(produtosEntrada);

                WebHookService.post(json);

                // Se houve resultados, envia a lista no formato HTML
                if (!produtosEntrada.isEmpty()) {
                    JapeSession.open();

                    JapeWrapper email = JapeFactory.dao("MSDFilaMensagem");

                    DynamicVO cabApoVO = email.create()
                            .set("STATUS", "Pendente")
                            .set("CODCON", BigDecimal.valueOf(0))
                            .set("TENTENVIO", BigDecimal.valueOf(1))
                            .set("MENSAGEM", emailHtml.toCharArray())
                            .set("TIPOENVIO", "E")
                            .set("MAXTENTENVIO", BigDecimal.valueOf(3))
                            .set("NUANEXO", null)
                            .set("ASSUNTO", "Entradas no Estoque Profood - " + dataPeriodo)
                            //.set("EMAIL", "yanprofood@gmail.com,sistemas@profood.com.br")
                            //.set("EMAIL", "sistemas@profood.com.br,vinicius@profood.com.br")
                            .set("EMAIL", "sistemas@profood.com.br,pcp@profood.com.br,pcp2@profood.com.br,gustavo@profood.com.br,projetos@profood.com.br,vendas@profood.com.br,elys@profood.com.br,cassiano@profood.com.br")
                            .set("MIMETYPE", null)
                            .set("TIPODOC", null)
                            .set("CODUSU", BigDecimal.valueOf(0))
                            .set("NUCHAVE", null)
                            .set("CODUSUREMET", null)
                            .set("REENVIAR", "N")
                            .set("MSGERRO", null)
                            .set("CODSMTP", null)
                            .set("DHULTTENTA", null)
                            .set("DBHASHCODE", null)
                            .set("CODCONTASMS", null)
                            .set("CELULAR", null)
                            .save();

                    BigDecimal codFilaGerado = cabApoVO.asBigDecimal("CODFILA");

                    WebHookService.post("{cod gerado: " + codFilaGerado + "}");
                }
                // }
            }
        } catch (Exception e) {
            WebHookService.post("{\"Deu erro: "+ e +"\"}");
        } finally {
            JapeSession.close();
        }
    }



    /**
     * Gera um HTML de notificação de pedidos.
     * Todos os estilos são definidos como classes dentro de <style> e aplicados via class="...".
     * O layout é baseado em <table> para garantir maior compatibilidade em diversos clientes de e-mail.
     */
    private String gerarHtmlPedidosSemInline(List<ProdutoEntrada> produtoEntradas) {
        // Início do código que gera o HTML para envio por email
        StringBuilder sb = new StringBuilder();

        // ---------------------------------------------------------------------------
        // 1) Agrupar a lista original por nota de entrada
        // ---------------------------------------------------------------------------
        Map<Integer, List<ProdutoEntrada>> mapNotas = produtoEntradas.stream()
                .collect(Collectors.groupingBy(ProdutoEntrada::getNotaEntrada));

        // ---------------------------------------------------------------------------
        // INÍCIO DO HTML
        // ---------------------------------------------------------------------------
        sb.append("<!DOCTYPE html>");
        sb.append("<html lang=\"pt-BR\">");

        // ---------------------------------------------------------------------------
        // CABEÇALHO (HEAD) - Metadados, Título, Estilos
        // ---------------------------------------------------------------------------
        sb.append("<head>");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        sb.append("<title>Notificação de Pedidos</title>");
        sb.append("<style type=\"text/css\">");
        // ---------------------------------------------------------------------------
        // ESTILOS E RESPONSIVIDADE
        // ---------------------------------------------------------------------------

        sb.append("@media only screen and (max-width: 600px) {");
        sb.append("    .stack-column {");
        sb.append("        display: block !important;");
        sb.append("        width: 100% !important;");
        sb.append("        margin-bottom: 10px !important;");
        sb.append("        border: 0 !important;");
        sb.append("    }");  // Fecha .stack-column
        sb.append("    .stack-column-pedidos {");
        sb.append("        max-height: 300px !important;");
        sb.append("        max-width: 300px !important;");
        sb.append("        padding-left: 15px !important;");
        sb.append("    }");  // Fecha .stack-column-pedidos
        sb.append("    .nome-cliente {");
        sb.append("        font-size: 10px;");
        sb.append("    }");  // Fecha .nome-cliente
        sb.append("    img.responsive-img {");
        sb.append("        max-width: 100% !important;");
        sb.append("        height: auto !important;");
        sb.append("    }");  // Fecha img.responsive-img
        sb.append("}");


        sb.append("</style>");
        sb.append("</head>");

        // ---------------------------------------------------------------------------
        // CORPO (BODY) - Estrutura Principal do Email
        // ---------------------------------------------------------------------------
        sb.append("<body style=\"background-color:#fafafa;font-family:'Inter', Arial, sans-serif;\">");
        sb.append("<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" align=\"center\" style=\"width:100%; margin:20px auto; background-color:#ffffff; padding:24px; border-radius:8px; box-shadow:0 1px 3px rgba(255, 1, 141, 0.1);\">");
        sb.append("<tr><td>");

        // ---------------------------------------------------------------------------
        // SEÇÃO DO LOGO
        // ---------------------------------------------------------------------------
        sb.append("<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"text-align:center; padding-bottom:16px;\">");
        sb.append("<tr><td align=\"center\">");
        sb.append("<img src=\"https://imgur.com/uyCBGU6.png\" alt=\"Logo da Empresa\" style=\"max-width:120px; height:auto;\">");
        sb.append("</td></tr>");
        sb.append("</table>");

        // ---------------------------------------------------------------------------
        // TÍTULO DO EMAIL
        // ---------------------------------------------------------------------------
        sb.append("<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"text-align:center; margin-bottom:24px;\">");
        sb.append("<tr><td align=\"center\" style=\"font-size:20px; font-weight:600; color:#222;\">Produtos Recebidos</td></tr>");
        sb.append("</table>");


        // ---------------------------------------------------------------------------
        // APLICAÇÃO DA LÓGICA (até passo 4)
        // ---------------------------------------------------------------------------
        /*
         *   Agora, para cada nota de entrada encontrada, geramos um bloco HTML.
         *   Dentro de cada nota, agrupamos os produtos (codigoProduto) e mostramos
         *   um “sub-bloco” para cada produto. (Ainda sem detalhar pedidos, passo 5)
         */
        for (Map.Entry<Integer, List<ProdutoEntrada>> entryNota : mapNotas.entrySet()) {
            Integer nota = entryNota.getKey();
            List<ProdutoEntrada> produtosDaNota = entryNota.getValue();

            // Conta quantos códigos de produto diferentes existem nessa nota
            long quantidadeProdutosDistintos = produtosDaNota.stream()
                    .map(ProdutoEntrada::getCodigoProduto)
                    .distinct()
                    .count();

            // ---------------------------------------------------------------------------
            // RESUMO GERAL (NÚMERO DA NOTA, QUANTIDADE DE PRODUTOS, ETC.)
            // ---------------------------------------------------------------------------
            sb.append("<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#f9f9f9; margin-bottom: 15px; border:1px solid #e0e0e0; border-top: 6px solid #E71D2D; border-radius:8px 8px 8px 8px; padding-top:6px; padding-left:16px; padding-right:16px; padding-bottom:6px;\">");
            sb.append("     <tr><td>");
            sb.append("     <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"border-bottom:1px solid #e0e0e0; padding-bottom:6px;\">");
            sb.append("          <tr>");

            // Número da Nota
            sb.append("               <td class=\"stack-column\" width=\"50%\" style=\"padding:8px; text-align:center;\">");
            sb.append("                    <span style=\"font-size:14px; color:#666; display:block; margin-bottom:4px;\">Número da Nota</span>");
            sb.append("                    <span style=\"font-size:18px; font-weight:600; color:#222; display:block;\">").append(nota).append("</span>");
            sb.append("               </td>");

            // Quantidade de Produtos
            sb.append("               <td class=\"stack-column\" width=\"50%\" style=\"padding:8px; text-align:center; border-left:1px solid #e0e0e0;\">");
            sb.append("                   <span style=\"font-size:14px; color:#666; display:block; margin-bottom:4px;\">Quantidade de Produtos</span>");
            sb.append("                    <span style=\"font-size:18px; font-weight:600; color:#222; display:block;\">").append(quantidadeProdutosDistintos).append("</span>"); // Aqui você concatena corretamente "Itens"
            sb.append("               </td>");
            sb.append("          </tr>");
            sb.append("     </table>");
            sb.append("     </td></tr>");

            // 3) Agrupar produtos dessa nota
            Map<Integer, List<ProdutoEntrada>> mapProdutos = produtosDaNota.stream()
                    .collect(Collectors.groupingBy(ProdutoEntrada::getCodigoProduto));

            // ---------------------------------------------------------------------------
            // LISTA DE PRODUTOS E DETALHES DOS PEDIDOS
            // ---------------------------------------------------------------------------



            // 4) Para cada produto agrupado
            for (Map.Entry<Integer, List<ProdutoEntrada>> entryProduto : mapProdutos.entrySet()) {
                Integer codProduto = entryProduto.getKey();
                List<ProdutoEntrada> listaOcorrencias = entryProduto.getValue();

                // Pegamos a descrição de um dos registros
                ProdutoEntrada algumRegistro = listaOcorrencias.get(0);
                String descricaoProduto = algumRegistro.getDescricaoProduto();

                int quantidadeRecebidaDoProduto = listaOcorrencias.get(0).getQuantidadeEntrada();

                sb.append("<tr>");
                sb.append("    <td>");
                sb.append("        <table width=\"100%\"");
                sb.append("            style=\"font-size:14px; font-weight:500; color:#444; margin-bottom:12px; background-color:#ffffff; padding-top:4px; padding-left:8px; padding-right:8px; border-left:4px solid #16C47F; border-radius:5px 0 0 5px; border-right:1px solid #e0e0e0; border-top:1px solid #e0e0e0; border-bottom:1px solid #e0e0e0;\">");
                sb.append("            <tr>");
                sb.append("                <td><span");
                sb.append("                        style=\"font-size:16px; font-weight:600; color:#222; margin-bottom:4px; display:block;\">");
                sb.append(codProduto).append(" - ").append(descricaoProduto);
                sb.append("                        </span></td>");
                sb.append("                <td");
                sb.append("                    style=\"font-size:16px; font-weight:600; color:#222; margin-bottom:4px; display:block; text-align:right;\">");
                sb.append("                    <span");
                sb.append("                        style=\"text-align:end; background-color:#FF9D23; border-radius:10px; color:white; padding:2px 8px;\">");
                sb.append(quantidadeRecebidaDoProduto).append(" "+listaOcorrencias.get(0).getVolumeRecebido());
                sb.append("                        </span>");
                sb.append("                </td>");
                sb.append("            </tr>");

                //Ordena lista por top
                Map<Integer, List<ProdutoEntrada>> mapTops = listaOcorrencias.stream()
                        .collect(Collectors.groupingBy(ProdutoEntrada::getCodTop));

                    for (Map.Entry<Integer, List<ProdutoEntrada>> entryTop : mapTops.entrySet()) {
                        Integer codTop = entryTop.getKey();
                        List<ProdutoEntrada> listaPorTop = entryTop.getValue();

                        ProdutoEntrada regTop = listaPorTop.get(0);
                        String descrTop = regTop.getDescrTop();

                        sb.append("<tr>");
                        sb.append("    <td style=\"padding-left: 30px; padding-bottom: 4px;\">");
                        sb.append("        <table class=\"stack-column-pedidos\" style=\"border-left:4px solid #1a62ff; border-radius:5px; width: 100%;\">");
                        sb.append("            <tr>");
                        sb.append("                <td>");
                        sb.append("                    <span style=\"font-size:15px; font-weight:600; color:#757575; margin-bottom:4px; display:block; padding-left: 8px;\">");
                        sb.append(codTop).append(" - ").append(descrTop);
                        sb.append("                    </span>");

                        // 👉 Filtra pedidos válidos (com notaVenda != 0)
                        List<ProdutoEntrada> pedidosComNota = listaPorTop.stream()
                                .filter(ped -> ped.getNotaVenda() != 0)
                                .collect(Collectors.toList());

                        if (!pedidosComNota.isEmpty()) {
                            for (ProdutoEntrada ped : pedidosComNota) {
                                sb.append("    <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin-left: 40px; padding:2px 4px; margin-bottom:4px; background-color:#f9f9f9; border-left:4px solid #FFD65A; border-radius:5px;\">");
                                sb.append("        <tr>");
                                sb.append("            <td style=\"color:#555; padding-right:15px; text-align: center;\">Pedido: <b style=\"color:#222;\">").append(ped.getNotaVenda()).append("</b></td>");
                                sb.append("            <td style=\"color:#555; padding-right:15px; text-align: center;\">Cliente: <b class=\"nome-cliente\" style=\"color:#222;\">").append(ped.getMatriz()).append("</b></td>");
                                sb.append("            <td style=\"color:#555; padding-right:15px; text-align: center;\">Quantidade: <b style=\"color:#222;\">").append(ped.getQuantidadePedidoVenda()).append(" ").append(ped.getVolumeProduto()).append("</b></td>");
                                sb.append("            <td style=\"color:#555; text-align: center;\">Emissão: <b style=\"color:#222;\">").append(ped.getDataPedido()).append("</b></td>");
                                sb.append("        </tr>");
                                sb.append("    </table>");
                            }
                        } else {
                            // 👉 Exibe mensagem quando não há pedidos vinculados
                            sb.append("    <div style=\"margin-left: 40px; padding: 4px; font-size:14px; color:#FF6B6B; font-weight: 500;\">PRODUTO SEM PEDIDO VINCULADO</div>");
                        }

                        sb.append("                </td>");
                        sb.append("            </tr>");
                        sb.append("        </table>");
                        sb.append("    </td>");
                        sb.append("</tr>");
                }
                sb.append("        </table>");
                sb.append("    </td>");
                sb.append("</tr>");
            }
            sb.append("</table>");
        }

        sb.append("</td></tr>");
        sb.append("</table>");
        sb.append("</body>");
        sb.append("</html>");

        return sb.toString();
    }
}