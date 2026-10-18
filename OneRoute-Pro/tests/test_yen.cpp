#include "transit/engine.hpp"
#include <algorithm>
#include <functional>
#include <iostream>
#include <random>
#include <set>
#include <stdexcept>
using namespace transit;

static void require(bool condition, const char* message) {
 if (!condition) throw std::runtime_error(message);
}

int main() {
 // Independent exhaustive station-simple-path enumeration, including parallel services.
 std::mt19937 rng(20260915);
 int comparisons=0;
 for (int trial=0;trial<150;++trial) {
  const int n=6;
  std::vector<Edge> edges;
  std::vector<int> waits={0,7,11};
  for (int a=0;a<n;++a) for(int b=a+1;b<n;++b) {
   if (rng()%100>=65) continue;
   int line=static_cast<int>(rng()%3);
   edges.push_back({static_cast<int>(edges.size()),a,b,line,1+static_cast<int>(rng()%40),line?static_cast<int>(rng()%20):0});
   if(rng()%5==0) {
    line=(line+1)%3;
    edges.push_back({static_cast<int>(edges.size()),a,b,line,1+static_cast<int>(rng()%40),line?static_cast<int>(rng()%20):0});
   }
  }
  Engine engine(n,2,edges,waits);
  Query q;q.source=0;q.destination=5;
  if(trial%7==0 && !edges.empty()) q.closedEdges={0};
  if(trial%11==0) q.closedStations={3};
  std::vector<Route> all;
  std::set<int> visited={0};
  std::function<void(int,int,bool,Route)> dfs=[&](int u,int last,bool walked,Route r) {
   if(u==5){r.found=true;all.push_back(r);return;}
   for(const Edge& e:edges) {
    int v=e.from==u?e.to:e.to==u?e.from:-1;
    if(v<0 || visited.count(v)) continue;
    if(std::find(q.closedEdges.begin(),q.closedEdges.end(),e.id)!=q.closedEdges.end())continue;
    if(std::find(q.closedStations.begin(),q.closedStations.end(),v)!=q.closedStations.end())continue;
    Route next=r;
    bool board=e.service && (e.service!=last||walked);
    next.seconds+=e.seconds+(board?waits[e.service]:0);
    next.fare+=e.fare;
    next.transfers+=board&&last!=0;
    next.edges.push_back(e.id);
    visited.insert(v);
    dfs(v,e.service?e.service:last,e.service?false:true,next);
    visited.erase(v);
   }
  };
  dfs(0,0,false,Route{});
  for(int objective=0;objective<4;++objective) {
   q.objective=objective;
   std::sort(all.begin(),all.end(),[&](const Route&a,const Route&b){return engine.score(a,objective)<engine.score(b,objective);});
   auto found=engine.alternatives(q,3);
   require(found.size()==std::min<std::size_t>(3,all.size()),"wrong number of alternatives");
   std::set<std::vector<int>> unique;
   for(std::size_t i=0;i<found.size();++i) {
    require(engine.score(found[i],objective)==engine.score(all[i],objective),"Yen rank disagrees with exhaustive oracle");
    require(unique.insert(found[i].edges).second,"duplicate alternative");
    require(std::set<int>(found[i].stations.begin(),found[i].stations.end()).size()==found[i].stations.size(),"station cycle");
    require(found[i].seconds==found[i].rideSeconds+found[i].walkSeconds+found[i].waitSeconds,"time accounting");
   }
   ++comparisons;
  }
 }
 Engine tree(3,1,{{0,0,1,1,4,2},{1,1,2,1,5,2}},{0,3});
 Query same;same.source=1;same.destination=1;
 require(tree.alternatives(same,3).size()==1,"same station should have one zero-length route");
 same.closedStations={1};require(tree.alternatives(same,3).empty(),"closed endpoint");
 std::cout<<"Yen oracle passed: "<<comparisons<<" ranked comparisons across 150 generated multimodal graphs.\n";
}
