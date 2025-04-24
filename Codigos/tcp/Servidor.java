import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

class Servidor {
   private static int portaServidor = 6790;
   private static int contadorMensagens = 0;

   public static void main(String argv[]) throws Exception {
      // Cria o socket do servidor
      ServerSocket socket = new ServerSocket(portaServidor);

      System.out.println("=== Servidor de Chat TCP ===");
      System.out.println("Aguardando conexões na porta " + portaServidor + "...");

      while (true) {
         // Aguarda conexão do cliente
         Socket conexao = socket.accept();
         contadorMensagens++;

         // Recebe a mensagem do cliente
         BufferedReader entrada = new BufferedReader(new InputStreamReader(conexao.getInputStream()));
         String mensagem = entrada.readLine();

         // Obtém o horário atual formatado
         SimpleDateFormat formato = new SimpleDateFormat("HH:mm:ss");
         String horario = formato.format(new Date());

         // Mostra a mensagem recebida no console do servidor
         System.out.println("[" + horario + "] Mensagem #" + contadorMensagens + ": " + mensagem);

         // Prepara a resposta para o cliente
         String resposta = "[MSG #" + contadorMensagens + " às " + horario + "] " + mensagem.toUpperCase();

         // Envia a resposta para o cliente
         DataOutputStream saida = new DataOutputStream(conexao.getOutputStream());
         saida.writeBytes(resposta + '\n');

         // Fecha a conexão com este cliente
         conexao.close();
      }
   }
}