package utils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebHookService {

    public static void post(String message) {
        String webhookUrl = "https://brainy-airplane-74.webhook.cool"; // Substitua pelo seu webhook
        String payload = message; // Corpo do POST com a mensagem passada como parâmetro

        try {
            // Configurando a URL e conexão
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Enviando o corpo do POST
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Verificando a resposta
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                System.out.println("POST enviado com sucesso para o webhook.");
            } else {
                System.out.println("Falha ao enviar POST. Código de resposta: " + responseCode);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar POST para o webhook: ", e);
        }
    }

    public static void main(String[] args) {
        WebHookService.post("Mensagem personalizada para o webhook!");
    }
}
