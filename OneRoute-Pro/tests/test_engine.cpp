#include "transit/engine.hpp"
#include <cstdlib>
#include <iostream>
#include <stdexcept>
using namespace transit;
void require(bool value,const char*message){if(!value){std::cerr<<"FAIL: "<<message<<'\n';std::exit(1);}}
int main(){
 {Engine e(4,2,{{0,0,1,1,100,100},{1,1,3,1,100,100},{2,0,2,2,10,500},{3,2,3,2,10,500}},{0,0,0});
 Query q;q.destination=3;auto r=e.route(q);require(r.seconds==20,"fastest chooses lower duration");q.objective=1;require(e.route(q).fare==200,"cheapest chooses lower fare");
 q.objective=0;q.closedStations={2};require(e.route(q).seconds==200,"station closure reroutes");q.closedStations={};q.closedEdges={2};require(e.route(q).seconds==200,"edge closure reroutes");
 q.closedEdges={};q.closedServices={1,2};require(!e.route(q).found,"all services closed");q.closedServices={};q.source=3;q.destination=0;require(e.route(q).seconds==20,"undirected reverse traversal");q.source=0;q.destination=0;require(e.route(q).seconds==0,"same station");q.source=-1;require(!e.route(q).found,"invalid source");}
 {Engine e(3,2,{{0,0,2,1,100000,100},{1,0,1,1,1,100},{2,1,2,2,1,100}},{0,0,0});Query q;q.destination=2;q.objective=2;auto r=e.route(q);require(r.transfers==0&&r.seconds==100000,"fewest transfers is lexicographic, no magic multiplier");}
 {Engine e(3,2,{{0,0,2,1,100000,100},{1,0,1,1,1,100},{2,1,2,1,1,1}},{0,0,0});Query q;q.destination=2;q.objective=1;require(e.route(q).fare==100,"time cannot override a cheaper fare");}
 {Engine e(3,2,{{0,0,1,1,1,0},{1,0,1,2,5,0},{2,1,2,2,1,0}},{0,100,100});Query q;q.destination=2;auto r=e.route(q);require(r.seconds==106&&r.edges.front()==1,"service state preserves the globally best arrival");}
 {Engine e(4,2,{{0,0,1,1,10,20},{1,1,2,0,30,0},{2,2,3,1,10,20}},{0,60,60});Query q;q.destination=3;auto r=e.route(q);require(r.transfers==1&&r.waitSeconds==120&&r.walkSeconds==30,"walk then same-service reboarding");require(r.seconds==r.rideSeconds+r.walkSeconds+r.waitSeconds,"time accounting");}
 {Engine e(3,3,{{0,0,1,3,10,0}},{0,0,0,0});Query q;q.destination=1;q.mode=1;require(!e.route(q).found,"metro mode excludes suburban");q.mode=2;require(e.route(q).found,"rail mode allows suburban");q.destination=2;require(!e.route(q).found,"unreachable destination");}
 {bool thrown=false;try{Engine e(2,2,{{0,0,1,1,-1,0}},{0,0,0});}catch(const std::invalid_argument&){thrown=true;}require(thrown,"negative edge rejected");}
 std::cout<<"All C++ behavioral tests passed (17 checks).\n";
}
