#include "transit/engine.hpp"
#include <algorithm>
#include <chrono>
#include <limits>
#include <queue>
#include <stdexcept>
#include <tuple>
#include <set>
namespace transit {
Engine::Engine(int n,int s,std::vector<Edge> edges,std::vector<int> waits)
 :n_(n),services_(s),edges_(std::move(edges)),adj_(n),waits_(std::move(waits)) {
 if(n<1||s<1||waits_.size()!=static_cast<std::size_t>(s+1)) throw std::invalid_argument("invalid graph dimensions");
 for(int w:waits_) if(w<0)throw std::invalid_argument("negative wait");
 for(const auto& e:edges_){
  if(e.id<0||e.id>=static_cast<int>(edges_.size())||e.from<0||e.from>=n||e.to<0||e.to>=n||e.service<0||e.service>s||e.seconds<=0||e.fare<0)throw std::invalid_argument("invalid edge");
  adj_[e.from].push_back(e); auto reverse=e; std::swap(reverse.from,reverse.to);adj_[e.to].push_back(reverse);
 }
}
Route Engine::route(const Query& q)const {
 const auto start=std::chrono::steady_clock::now(); Route r;
 auto finish=[&](){r.micros=std::chrono::duration<double,std::micro>(std::chrono::steady_clock::now()-start).count();return r;};
 if(q.source<0||q.source>=n_||q.destination<0||q.destination>=n_||q.objective<0||q.objective>3||q.mode<0||q.mode>2)return finish();
 std::vector<bool> closed(n_,false), blocked(edges_.size(),false), services(services_+1,false);
 for(int v:q.closedStations)if(v>=0&&v<n_)closed[v]=true;
 for(int e:q.closedEdges)if(e>=0&&e<static_cast<int>(blocked.size()))blocked[e]=true;
 for(int s:q.closedServices)if(s>0&&s<=services_)services[s]=true;
 if(closed[q.source]||closed[q.destination])return finish();
 // State = (station, last boarded service, walked since last boarding).
 // Remembering walking means reboarding the SAME service still incurs a new wait.
 const int width=(services_+1)*2, count=n_*width;
 auto state=[&](int node,int line,int walked){return node*width+line*2+walked;};
 const std::int64_t inf=std::numeric_limits<std::int64_t>::max()/8;
 std::vector<Cost>d(count,Cost{inf,inf,inf});
 struct Parent{int state=-1,edge=-1,wait=0;}; std::vector<Parent> parent(count);
 using Item=std::pair<Cost,int>;std::priority_queue<Item,std::vector<Item>,std::greater<Item>>pq;
 int root=state(q.source,q.initialService,q.initialWalked);d[root]={0,0,0};pq.push({d[root],root});int target=-1;
 while(!pq.empty()){
  auto [cost,u]=pq.top();pq.pop();if(cost!=d[u])continue;
  ++r.expanded;int station=u/width,last=(u%width)/2,walked=u%2;
  // With nonnegative lexicographic edge costs, the first destination popped is optimal.
  if(station==q.destination){target=u;break;}
  for(const auto&e:adj_[station]){
   if(closed[e.to]||blocked[e.id]||services[e.service])continue;
   if(e.service && ((q.mode==1&&e.service>2)||(q.mode==2&&e.service<=2)))continue;
   ++r.relaxed;
   bool boarding=e.service && (last!=e.service||walked);
   int transfer=boarding&&last!=0,wait=boarding?waits_[e.service]:0;
   std::int64_t time=e.seconds+wait;Cost delta;
   if(q.objective==0)delta={time,transfer,e.fare};
   else if(q.objective==1)delta={e.fare,time,transfer};
   else if(q.objective==2)delta={transfer,time,e.fare};
   else delta={time*5+e.fare+transfer*900,time,e.fare};
   Cost next={cost[0]+delta[0],cost[1]+delta[1],cost[2]+delta[2]};
   int v=state(e.to,e.service?e.service:last,e.service?0:1);
   if(next<d[v]){d[v]=next;parent[v]={u,e.id,wait};pq.push({next,v});}
  }
 }
 if(target<0)return finish();
 r.found=true;
 for(int u=target;u!=root;u=parent[u].state){
  r.stations.push_back(u/width);r.edges.push_back(parent[u].edge);r.waits.push_back(parent[u].wait);
 }
 r.stations.push_back(q.source);std::reverse(r.stations.begin(),r.stations.end());
 std::reverse(r.edges.begin(),r.edges.end());std::reverse(r.waits.begin(),r.waits.end());
 int last=q.initialService;bool walked=q.initialWalked;
 for(std::size_t i=0;i<r.edges.size();++i){
  const auto&e=edges_[r.edges[i]];r.fare+=e.fare;r.waitSeconds+=r.waits[i];
  if(e.service){if(last&&(last!=e.service||walked))++r.transfers;last=e.service;walked=false;r.rideSeconds+=e.seconds;}
  else {r.walkSeconds+=e.seconds;walked=true;}
 }
 r.seconds=r.rideSeconds+r.walkSeconds+r.waitSeconds;return finish();
}

Cost Engine::score(const Route& r, int objective) const {
 if (objective==0) return {r.seconds,r.transfers,r.fare};
 if (objective==1) return {r.fare,r.seconds,r.transfers};
 if (objective==2) return {r.transfers,r.seconds,r.fare};
 return {5*r.seconds+r.fare+900*r.transfers,r.seconds,r.fare};
}

Route Engine::account(const Query& q, const std::vector<int>& edgeIds) const {
 Route result;
 result.found=true;
 result.edges=edgeIds;
 result.stations.push_back(q.source);
 int station=q.source, last=q.initialService;
 bool walked=q.initialWalked;
 for (int id:edgeIds) {
  const auto& edge=edges_.at(id);
  if (edge.from!=station && edge.to!=station) throw std::logic_error("disconnected path");
  station=edge.from==station?edge.to:edge.from;
  result.stations.push_back(station);
  const bool boarding=edge.service && (last!=edge.service || walked);
  const int wait=boarding?waits_[edge.service]:0;
  result.waits.push_back(wait);
  result.waitSeconds+=wait;
  result.fare+=edge.fare;
  if (edge.service) {
   result.rideSeconds+=edge.seconds;
   if (boarding && last) ++result.transfers;
   last=edge.service;
   walked=false;
  } else {
   result.walkSeconds+=edge.seconds;
   walked=true;
  }
 }
 result.seconds=result.rideSeconds+result.walkSeconds+result.waitSeconds;
 return result;
}

// Yen's root/spur decomposition with the service context carried into each spur.
// Candidate heap persists across iterations. Paths are distinct by edge sequence.
std::vector<Route> Engine::alternatives(const Query& q, int k) const {
 if (k<1 || k>3) throw std::invalid_argument("k must be between 1 and 3");
 std::vector<Route> accepted;
 Route best=route(q);
 if (!best.found) return accepted;
 accepted.push_back(best);
 auto worse=[&](const Route& a,const Route& b) {
  if (score(a,q.objective)!=score(b,q.objective)) return score(a,q.objective)>score(b,q.objective);
  return a.edges>b.edges;
 };
 std::priority_queue<Route,std::vector<Route>,decltype(worse)> candidates(worse);
 std::set<std::vector<int>> seen;
 seen.insert(best.edges);
 for (int rank=1;rank<k;++rank) {
  const Route previous=accepted.back();
  int last=q.initialService;
  bool walked=q.initialWalked;
  for (std::size_t spurIndex=0;spurIndex<previous.edges.size();++spurIndex) {
   Query spur=q;
   spur.source=previous.stations[spurIndex];
   spur.initialService=last;
   spur.initialWalked=walked;
   spur.closedStations.insert(spur.closedStations.end(),previous.stations.begin(),previous.stations.begin()+spurIndex);
   for (const auto& path:accepted) {
    if (path.edges.size()>spurIndex && std::equal(previous.edges.begin(),previous.edges.begin()+spurIndex,path.edges.begin())) {
     spur.closedEdges.push_back(path.edges[spurIndex]);
    }
   }
   Route suffix=route(spur);
   if (suffix.found) {
    std::vector<int> combined(previous.edges.begin(),previous.edges.begin()+spurIndex);
    combined.insert(combined.end(),suffix.edges.begin(),suffix.edges.end());
    if (seen.insert(combined).second) {
     Route candidate=account(q,combined);
     // With static service-wide waits, a shortest spur never benefits from a station cycle.
     std::set<int> unique(candidate.stations.begin(),candidate.stations.end());
     if (unique.size()!=candidate.stations.size()) throw std::logic_error("non-simple spur path");
     candidates.push(std::move(candidate));
    }
   }
   const int service=edges_[previous.edges[spurIndex]].service;
   if (service) {last=service;walked=false;} else {walked=true;}
  }
  if (candidates.empty()) break;
  accepted.push_back(candidates.top());
  candidates.pop();
 }
 return accepted;
}
}
