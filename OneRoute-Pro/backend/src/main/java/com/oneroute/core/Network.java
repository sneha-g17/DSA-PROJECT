package com.oneroute.core;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.io.IOException;
import java.util.*;

/** Loads demo data and compiles a compact graph for the native C++ process. */
public final class Network {
    public final Map<String,Object> data;
    public final List<Object> stations, services, edges;
    public final String revision;
    public Network(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        data = Json.object(Json.parse(new String(bytes, StandardCharsets.UTF_8)));
        if (integer(data.get("schema_version"),3,3) != 3) throw new IllegalArgumentException("Invalid schema");
        stations = Json.array(data.get("stations")); services = Json.array(data.get("services")); edges = Json.array(data.get("edges"));
        integer(stations.size(),1,10000); integer(services.size(),2,16); integer(edges.size(),1,100000);
        Set<String> names = new HashSet<>(), connections = new HashSet<>();
        for (int i=0;i<stations.size();i++) {
            var st=Json.object(stations.get(i)); integer(st.get("id"),i,i);
            if (!(st.get("name") instanceof String name) || name.isBlank() || !names.add(name)) throw new IllegalArgumentException("Invalid station name");
            if (!(st.get("accessibility") instanceof String) || !List.of("unknown","yes","no").contains(st.get("accessibility"))) throw new IllegalArgumentException("Invalid accessibility");
        }
        for (int i=0;i<services.size();i++) {
            var service=Json.object(services.get(i)); integer(service.get("id"),i+1,i+1); integer(service.get("wait_seconds"),0,3600);
            for(Object stop:Json.array(service.get("stops"))) integer(stop,0,stations.size()-1);
        }
        for (int i=0;i<edges.size();i++) {
            var edge=Json.object(edges.get(i)); integer(edge.get("id"),i,i);
            int a=integer(edge.get("from"),0,stations.size()-1), b=integer(edge.get("to"),0,stations.size()-1);
            int service=integer(edge.get("service"),0,services.size());
            integer(edge.get("seconds"),1,86400); integer(edge.get("distance_m"),1,1000000);
            int fare=integer(edge.get("fare_paise"),0,100000);
            if(a==b || (service==0 && fare!=0) || !connections.add(Math.min(a,b)+":"+Math.max(a,b)+":"+service)) throw new IllegalArgumentException("Invalid connection");
        }
        try { revision=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).substring(0,16); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        data.put("revision",revision);
    }
    public static int integer(Object value,int low,int high) {
        if (!(value instanceof Integer || value instanceof Long)) throw new IllegalArgumentException("Expected integer");
        long n=((Number)value).longValue();
        if(n<low || n>high) throw new IllegalArgumentException("Integer outside allowed range");
        return (int)n;
    }
    public void writeGraph(Path path) throws IOException {
        StringBuilder out=new StringBuilder().append(stations.size()).append(' ').append(services.size()).append(' ').append(edges.size()).append("\n0");
        for(Object service:services) out.append(' ').append(Json.object(service).get("wait_seconds"));
        out.append('\n');
        for(Object edge:edges) {
            var e=Json.object(edge);
            for(String key:List.of("from","to","service","seconds","fare_paise")) out.append(e.get(key)).append(' ');
            out.append('\n');
        }
        Files.writeString(path,out);
    }
    public int station(String name) {
        for(Object station:stations) {
            var st=Json.object(station);
            if(st.get("name").equals(name)) return ((Number)st.get("id")).intValue();
        }
        throw new IllegalArgumentException("Unknown station: "+name);
    }
}
