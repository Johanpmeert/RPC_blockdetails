package com.company;

import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;
import wf.bitcoin.krotjson.HexCoder;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Sha256Hash;

import static org.bitcoinj.core.Utils.sha256hash160;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import org.bouncycastle.util.encoders.Hex;

public class Main {
    public static void main(String[] args) {
        System.out.println("Demo how to extract a specific transaction from a Bitcoin block");
        System.out.println();
        System.out.print("Initialising ... ");
        final String protocol = "http";
        // On your bitcoin node (bitcoind or bitcoin-QT), check that bitcoin.conf contains:
        // server = 1 (otherwise no RPC)
        // txindex = 1  (this one is crucial and takes a long time to resync, it allows searching ANY transaction)
        // rpcallowip = x.x.x.x (fill here your ip address of the workstation where you are running this)
        // rpcport = 8332 (standard port)
        // rpcauth = username:specialcreatedpasswordhash (use google to lookup python scipt "rpcuser.py" and run from commandline to generate)
        final String rpcuser = "jswemmelbtc"; // insert your username here
        final String rpcpassword = "ZEHUGANovjdw7jJUuZZr1L2sa"; // insert your password here (not the hashed one)
        final String rpcaddress = "192.168.2.16"; // insert ip address to bitcoin node here
        final int rpcport = 8332; // insert bitcoin node RPC port number
        long startime;
        long stoptime;
        System.out.println("done");
        System.out.print("Creating URL ... ");
        URL rpcurl;
        try {
            rpcurl = new URL(protocol + "://" + rpcuser + ":" + rpcpassword + "@" + rpcaddress + ":" + rpcport + "/");
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL:" + e + "; exiting ...");
            return;
        }
        System.out.println("successfull");
        System.out.print("Opening connection to blockchain on bitcoin node @ " + rpcaddress + ":" + rpcport + " ... ");
        startime = System.nanoTime();
        BitcoinJSONRPCClient bitcoinClient = new BitcoinJSONRPCClient(rpcurl);
        stoptime = System.nanoTime();
        System.out.println(" done, that took " + (stoptime - startime) / 1e9 + " seconds");
        System.out.println();
        System.out.println("General Blockchain & node information:");
        System.out.println("\tYour node is connected to "+bitcoinClient.getConnectionCount()+" other nodes");
        System.out.println("\tYour node is version is "+bitcoinClient.getNetworkInfo().subversion());
        System.out.println("\tBitcoin mining difficulty: "+bitcoinClient.getBlockChainInfo().difficulty());
        System.out.println("\tTotal Bitcoin block count reported by node = " + bitcoinClient.getBlockCount());
        System.out.println("\tWallet balance of your node = " + bitcoinClient.getBalance());
        System.out.println();
        BitcoindRpcClient.Block BTCblock;
        int getblock;
        do {
            do {
                System.out.print("Get which block? (0 to stop) = ");
                Scanner in = new Scanner(System.in);
                getblock = in.nextInt();
            }
            while (getblock < 0 || getblock > bitcoinClient.getBlockCount());
            if (getblock == 0) return;
            System.out.print("Getting block " + getblock + " from Blockchain...");
            // First get block
            startime = System.nanoTime();
            BTCblock = bitcoinClient.getBlock(getblock);
            stoptime = System.nanoTime();
            System.out.println(" done, that took " + (stoptime - startime) / 1e9 + " seconds");
            // System.out.println("Rawblock = "+BTCblock);
            int aantaltrans = BTCblock.tx().size();
            System.out.println("The block " + getblock + " contains " + aantaltrans + " transactions");
            int transaction;
            if (aantaltrans == 1) {
                transaction = 0;
            } else {
                do {
                    System.out.print("Which transaction would you like to see? (0-" + (aantaltrans - 1) + "): ");
                    Scanner in = new Scanner(System.in);
                    transaction = in.nextInt();
                }
                while (transaction < 0 || transaction > aantaltrans-1);
            }
            startime = System.nanoTime();
            System.out.print("\tGetting Tx " + transaction + " from block " + getblock + " from Blockchain...");
            BitcoindRpcClient.RawTransaction testraw, testraw2;
            startime = System.nanoTime();
            testraw = bitcoinClient.getRawTransaction(BTCblock.tx().get(transaction));
            stoptime = System.nanoTime();
            System.out.println(" done, that took " + (stoptime - startime) / 1e9 + " seconds");
            if (transaction == 0) {
                // System.out.println("Complete = "+testraw);
                // System.out.println("\tFull tx = "+testraw.vOut().get(0));
                String checktype = testraw.vOut().get(0).scriptPubKey().type();
                System.out.print("\tThis is a Coinbase (mining) transaction of type " + checktype + ", " + testraw.vOut().get(0).value().toString());
                System.out.print(" BTC were awarded to address ");
                if (checktype.equals("pubkey")) {
                    // we only have scriptPubKey to work with ... assuming P2PK (Pay to Public key)
                    // so take the "hex" JSON, take off first byte (=coded length), take off last byte (byte "ac" = OP_CHECKSIG)
                    String hexstring = testraw.vOut().get(0).scriptPubKey().hex();
                    String hexstringfinal = hexstring.substring(2, hexstring.length() - 2);
                    System.out.println(PubkeyhexstringTobtcaddress(hexstringfinal));
                } else if (checktype.equals("scripthash") || checktype.equals("pubkeyhash")) {
                    // easy: address is in the Tx 0 JSON, so just get that and be done
                    System.out.println(testraw.vOut().get(0).scriptPubKey().addresses());
                }
            } else {
                double totalvin = 0.0;
                double totalvout = 0.0;
                int numberofvout = testraw.vOut().size();
                System.out.println("\tThere (is)are " + numberofvout + " Vout(s) in this transaction:");
                for (int teller = 0; teller < numberofvout; teller++) {
                    totalvout += testraw.vOut().get(teller).value().doubleValue();
                    System.out.print("\t\tVout value " + teller + ": " + testraw.vOut().get(teller).value().toString());
                    System.out.println(" BTC sent to address " + testraw.vOut().get(teller).scriptPubKey().addresses().toString());
                }
                int numberofvin = testraw.vIn().size();
                System.out.println("\tThere (is)are " + numberofvin + " Vin(s) in this transaction:");
                for (int teller = 0; teller < numberofvin; teller++) {
                    int voutweneed = testraw.vIn().get(teller).vout();
                    System.out.print("\t\tWe need Vout(" + voutweneed + ") from transaction txid ");
                    System.out.print(testraw.vIn().get(teller).txid() + " so ");
                    testraw2 = bitcoinClient.getRawTransaction(testraw.vIn().get(teller).txid());
                    totalvin += testraw2.vOut().get(voutweneed).value().doubleValue();
                    System.out.print(testraw2.vOut().get(voutweneed).value().toString());
                    if (testraw2.vOut().get(voutweneed).scriptPubKey().type().equals("pubkey")) {
                        // same here as with tx0, pubkey translation needed because no address field ...
                        String hexstring = testraw2.vOut().get(0).scriptPubKey().hex();
                        String hexstringfinal = hexstring.substring(2, hexstring.length() - 2);
                        System.out.println(" BTC came from address " + PubkeyhexstringTobtcaddress(hexstringfinal));
                    } else
                        System.out.println(" BTC came from address " + testraw2.vOut().get(voutweneed).scriptPubKey().addresses().toString());
                }
                System.out.printf("\tFee was %.8f", totalvin - totalvout);
                System.out.println(" BTC; " + totalvin + " - " + totalvout);
            }
            stoptime = System.nanoTime();
            System.out.println("\tDone, that took " + (stoptime - startime) / 1e9 + " seconds");
            System.out.println();
        }
        while (true);
    }

    public static String PubkeyhexstringTobtcaddress(String pubkey) {
        // we get Public Key as a hex String, to translate to readable BTC Address, we need to:
        // first ripemd160(sha-256(Public Key))
        // then add byte 00 in front
        // calculate sha256(sha256(the value with the 00 in front)), take first 4 bytes
        // make final string 00 + ripemd(160(sha256(Public Key)) + 4 bytes
        // Convertbase58 of this string == bitcoin address
        byte[] sha = Hex.decode(pubkey);
        // byte[] shadone = Sha256Hash.hash(sha); // not really needed
        // System.out.println("\t\t\tSHA256 = " +HexCoder.encode(shadone));
        byte[] ripemd160done = sha256hash160(sha);  // does the ripemd160 en sha256 in one go
        String addnull = "00" + HexCoder.encode(ripemd160done);
        // System.out.println("\t\t\t00 + ripemd160(sha256)) = "+addnull);
        byte[] shatwice = Sha256Hash.hashTwice(Hex.decode(addnull));
        // System.out.println("\t\t\tSHA-twice ="+HexCoder.encode(shatwice));
        String totalbtc = addnull + HexCoder.encode(shatwice).substring(0, 8);
        // System.out.println("\t\t\ttotal = "+totalbtc);
        return Base58.encode(Hex.decode(totalbtc));
        // System.out.println("\t\tBTC adress = "+btcaddress);
    }
}
