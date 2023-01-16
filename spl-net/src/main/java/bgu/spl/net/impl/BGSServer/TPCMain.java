package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.srv.BaseServer;
import bgu.spl.net.srv.Server;

public class TPCMain {
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        Server.threadPerClient(
                port, //port
                () -> new BidiMessagingProtocolImpl(), //protocol factory
                () -> new EncoderDecoder() //message encoder decoder factory
        ).serve();
    }
}
