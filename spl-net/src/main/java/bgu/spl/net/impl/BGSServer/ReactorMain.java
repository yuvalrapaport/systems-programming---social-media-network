package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.srv.Reactor;

public class ReactorMain {
    public static void main(String[] args) {
        int numOfThreads = Integer.parseInt(args[1]);
        int port = Integer.parseInt(args[0]);
        Reactor<String> reactor = new Reactor(
                numOfThreads,
                port,
                () ->  new BidiMessagingProtocolImpl(),
                () ->  new EncoderDecoder()
        );
        reactor.serve();
    }
}