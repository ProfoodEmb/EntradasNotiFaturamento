package Classes;

public class ProdutoEntrada {

    private Integer notaEntrada;
    private Integer codigoProduto;
    private String descricaoProduto;
    private Integer quantidadeEntrada;
    private Integer notaVenda;
    private Integer quantidadePedidoVenda;
    private Integer codTop;
    private String dataPedido;
    private String vendedor;
    private String email;
    private  String volume;
    private  String volumeRecebido;
    private  String matriz;
    private String descrTop;


    public ProdutoEntrada() {
    }

    public ProdutoEntrada(Integer notaEntrada, Integer codigoProduto, String descricaoProduto,
                          Integer quantidadeEntrada, Integer notaVenda, Integer quantidadePedidoVenda,
                          String dataPedido, String vendedor, String email, String volume, String matriz, String volumeRecebido, Integer codTop, String descrTop) {

        this.notaEntrada = notaEntrada;
        this.codigoProduto = codigoProduto;
        this.descricaoProduto = descricaoProduto;
        this.quantidadeEntrada = quantidadeEntrada;
        this.notaVenda = notaVenda;
        this.quantidadePedidoVenda = quantidadePedidoVenda;
        this.dataPedido = dataPedido;
        this.vendedor = vendedor;
        this.email = email;
        this.volume = volume;
        this.matriz = matriz;
        this.volumeRecebido = volumeRecebido;
        this.codTop = codTop;
        this.descrTop = descrTop;

    }

    public String getDescrTop() {
        return descrTop;
    }

    public void setDescrTop(String descrTop) {
        this.descrTop = descrTop;
    }

    public Integer getCodTop() {
        return codTop;
    }

    public void setCodTop(Integer codTop) {
        this.codTop = codTop;
    }

    public void setMatriz(String matriz){
        this.matriz = matriz;
    }

    public String getMatriz(){
        return matriz;
    }

    public void setVolumeRecebido(String volume){
        this.volume = volume;
    }

    public String getVolumeRecebido(){
        return volume;
    }

    public void setVolumeProduto(String volume){
        this.volume = volume;
    }

    public String getVolumeProduto(){
        return volume;
    }

    public Integer getNotaEntrada() {
        return notaEntrada;
    }

    public void setNotaEntrada(Integer notaEntrada) {
        this.notaEntrada = notaEntrada;
    }

    public Integer getCodigoProduto() {
        return codigoProduto;
    }

    public void setCodigoProduto(Integer codigoProduto) {
        this.codigoProduto = codigoProduto;
    }

    public String getDescricaoProduto() {
        return descricaoProduto;
    }

    public void setDescricaoProduto(String descricaoProduto) {
        this.descricaoProduto = descricaoProduto;
    }

    public Integer getQuantidadeEntrada() {
        return quantidadeEntrada;
    }

    public void setQuantidadeEntrada(Integer quantidadeEntrada) {
        this.quantidadeEntrada = quantidadeEntrada;
    }

    public Integer getNotaVenda() {
        return notaVenda;
    }

    public void setNotaVenda(Integer notaVenda) {
        this.notaVenda = notaVenda;
    }

    public Integer getQuantidadePedidoVenda() {
        return quantidadePedidoVenda;
    }

    public void setQuantidadePedidoVenda(Integer quantidadePedidoVenda) {
        this.quantidadePedidoVenda = quantidadePedidoVenda;
    }

    public String getDataPedido() {
        return dataPedido;
    }

    public void setDataPedido(String dataPedido) {
        this.dataPedido = dataPedido;
    }

    public String getVendedor() {
        return vendedor;
    }

    public void setVendedor(String vendedor) {
        this.vendedor = vendedor;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Retorna uma representação em JSON dos dados deste objeto.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"notaEntrada\":").append(notaEntrada == null ? "null" : notaEntrada).append(",");
        sb.append("\"codigoProduto\":").append(codigoProduto == null ? "null" : codigoProduto).append(",");
        sb.append("\"descricaoProduto\":\"").append(descricaoProduto == null ? "" : descricaoProduto).append("\",");
        sb.append("\"quantidadeEntrada\":").append(quantidadeEntrada == null ? "null" : quantidadeEntrada).append(",");
        sb.append("\"notaVenda\":").append(notaVenda == null ? "null" : notaVenda).append(",");
        sb.append("\"quantidadePedidoVenda\":").append(quantidadePedidoVenda == null ? "null" : quantidadePedidoVenda).append(",");
        sb.append("\"dataPedido\":\"").append(dataPedido == null ? "" : dataPedido).append("\",");
        sb.append("\"vendedor\":\"").append(vendedor == null ? "" : vendedor).append("\",");
        sb.append("\"email\":\"").append(email == null ? "" : email).append("\"");
        sb.append("}");
        return sb.toString();
    }
}