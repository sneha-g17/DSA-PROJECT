#include "transit/engine.hpp"
#include <fstream>
#include <iostream>
#include <sstream>
#include <stdexcept>
#include <string>
#include <chrono>
using namespace transit;
void output(const Route&r){
 std::cout<<"{\"found\":"<<(r.found?"true":"false")<<",\"seconds\":"<<r.seconds<<",\"ride_seconds\":"<<r.rideSeconds<<",\"walk_seconds\":"<<r.walkSeconds<<",\"wait_seconds\":"<<r.waitSeconds<<",\"fare_paise\":"<<r.fare<<",\"transfers\":"<<r.transfers<<",\"expanded\":"<<r.expanded<<",\"relaxed\":"<<r.relaxed<<",\"engine_us\":"<<r.micros;
 auto array=[](const char*key,const std::vector<int>&v){std::cout<<",\""<<key<<"\":[";for(std::size_t i=0;i<v.size();++i){if(i)std::cout<<',';std::cout<<v[i];}std::cout<<']';};
 array("stations",r.stations);array("edges",r.edges);array("waits",r.waits);std::cout<<"}";
}
int main(int argc,char**argv){
 try{
  if(argc!=2)throw std::runtime_error("Usage: transit_engine network.txt");
  std::ifstream in(argv[1]);int n,s,m;if(!(in>>n>>s>>m)||n<1||n>100000||s<1||s>32||m<0||m>1000000)throw std::runtime_error("bad graph header");
  std::vector<int>w(s+1);for(auto&v:w)if(!(in>>v))throw std::runtime_error("bad waits");
  std::vector<Edge>edges;for(int i=0;i<m;++i){Edge e;e.id=i;if(!(in>>e.from>>e.to>>e.service>>e.seconds>>e.fare))throw std::runtime_error("bad edge");edges.push_back(e);}
  Engine engine(n,s,edges,w);std::string line;
  while(std::getline(std::cin,line)){
   try{std::istringstream input(line);Query q;if(!(input>>q.source>>q.destination>>q.objective>>q.mode))throw std::runtime_error("invalid query");
    for(auto*list:{&q.closedStations,&q.closedEdges,&q.closedServices}){int count;if(!(input>>count)||count<0||count>100000)throw std::runtime_error("invalid list");for(int i=0;i<count;++i){int value;if(!(input>>value))throw std::runtime_error("missing value");list->push_back(value);}}
    int k=1;
    input>>std::ws;
    if (!input.eof() && (!(input>>k) || k<1 || k>3)) throw std::runtime_error("invalid k");
    std::string extra;if(input>>extra)throw std::runtime_error("extra input");
    if (k==1) {output(engine.route(q));std::cout<<'\n'<<std::flush;}
    else {
     auto begin=std::chrono::steady_clock::now();
     auto routes=engine.alternatives(q,k);
     auto elapsed=std::chrono::duration<double,std::micro>(std::chrono::steady_clock::now()-begin).count();
     std::cout<<"{\"routes\":[";
     for (std::size_t i=0;i<routes.size();++i) {if(i)std::cout<<',';output(routes[i]);}
     std::cout<<"],\"engine_us\":"<<elapsed<<"}\n"<<std::flush;
    }
   }catch(const std::exception&){std::cout<<"{\"error\":\"invalid query\"}\n"<<std::flush;}
  }
 }catch(const std::exception&e){std::cerr<<e.what()<<'\n';return 1;}
}
