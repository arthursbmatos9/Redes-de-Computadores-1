import java.io.*;
import java.net.*;

class Servidor {
   private static int portaServidor = 2223;
   private static byte[] receiveData = new byte[1024];
   private static byte[] sendData = new byte[1024];

   public static String verificarBancoDeDadosFake(String nome, int ip) {
      String resp = "-1";
      if (ip != 4 && ip != 6) {
         resp = "Versão inválida do Protocolo IP";
      } else if (nome.equals("www.pucminas.br")) {
         resp = (ip == 4) ? "200.229.32.28" : "2800:3f0:4004:805::200e";
      } else if (nome.equals("www.mec.gov.br")) {
         resp = (ip == 4) ? "200.130.2.135" : "2600:1419:d400:28f::3831";
      }
      return resp;
   }

   public static void main(String args[]) throws Exception {
      DatagramSocket serverSocket = new DatagramSocket(portaServidor);

      while (true) {
         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

         System.out.println("Aguardando datagrama do cliente....");
         serverSocket.receive(receivePacket);

         // System.out.println("RECEIVED: " + new String(receivePacket.getData()));

         String recebido = new String(receivePacket.getData());
         String[] dividido = recebido.split(" ");
         // System.out.println("end = " + dividido[0] + ". ip= " + dividido[1] + ".");
         String nome = dividido[0];
         int ip = Integer.parseInt(dividido[1].trim());
         String resp = verificarBancoDeDadosFake(nome, ip);

         InetAddress ipCliente = receivePacket.getAddress();
         int portaCliente = receivePacket.getPort();
         sendData = (resp).getBytes();

         DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipCliente, portaCliente);
         serverSocket.send(sendPacket);
         System.out.println("Enviado...");
      }
   }
}
