package com.sa.client.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpConstants;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Slf4j
@ToString(of = {"host", "port", "https", "version"})
public class HttpMiddleware {

    private String host;
    private int port;
    private Map<String, String> headers = new ConcurrentHashMap<>(30);
    private boolean https;
    private String version;
    private boolean complete;

    public void digest(Object msg){
        if(msg instanceof ByteBuf){
            ByteBuf in = (ByteBuf) msg;
            while(in.isReadable()){
                String line = readLine(in);
                if(line == null || line.trim() == null){
                    break ;
                }
                line = line.trim();
                log.info("line: {}", line);
                String[] lineSplit = line.split(" ");
                if( lineSplit.length == 3 && version == null ){
                    https = lineSplit[0].equalsIgnoreCase("CONNECT");
                    version = lineSplit[2].trim();
                    continue;
                }
                String[] header = line.split(":");
                if(line.startsWith("Host:")){
                    host = header[1].trim();
                    port = https ? 443 : 80;
                    headers.put(header[0], header[1].trim());
                }
                if(header.length == 2){
                    headers.put(header[0], header[1].trim());
                }
            }
            in.resetReaderIndex();
            // 读完之后可以做一些判断，验证之类的操作。
            log.info("source msg size: {}", in.toString());
            log.info("headers :{}", headers);
        }
    }

    // 读取head
    private String readLine(ByteBuf in) {
        StringBuffer sb = new StringBuffer();
        while(in.isReadable()){
            byte b = in.readByte();
            char nextByte = (char) (b & 0xFF);
            sb.append(nextByte);
            if(nextByte == HttpConstants.LF && sb.length() == 2){
                log.info("header read complete");
                complete = true;
                return null;
            }else if (nextByte == HttpConstants.LF) {
                return sb.toString();
            }
        }
        if(sb.length() > 0){
            log.info("result :{} ", sb.toString());
            return sb.toString();
        }
        log.info("no readable");
        return null;
    }

}
