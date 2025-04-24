import java.io.*;
import java.net.*;

class Cliente {
   private static String ipServidor = "127.0.0.1";
   private static int portaServidor = 6790;

   public static String lerString() throws Exception {
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      return in.readLine();
   }

   public static void main(String argv[]) throws Exception {
      boolean continuar = true;

      System.out.println("=== Cliente de Chat TCP ===");
      System.out.println("Digite 'sair' para encerrar o chat");

      while (continuar) {
         System.out.print("Mensagem: ");
         String mensagem = lerString();

         if (mensagem.equalsIgnoreCase("sair")) {
            continuar = false;
            System.out.println("Encerrando o chat...");
            break;
         }

         // Estabelece conexão com o servidor
         Socket socket = new Socket(ipServidor, portaServidor);

         // Envia a mensagem para o servidor
         DataOutputStream saida = new DataOutputStream(socket.getOutputStream());
         saida.writeBytes(mensagem + '\n');

         // Recebe a resposta do servidor
         BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         String resposta = entrada.readLine();
         System.out.println("Servidor: " + resposta);

         // Fecha a conexão
         socket.close();
      }
   }
}