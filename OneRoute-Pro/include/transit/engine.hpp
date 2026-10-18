#pragma once
#include <array>
#include <cstdint>
#include <vector>
namespace transit {
using Cost = std::array<std::int64_t,3>;
struct Edge { int id, from, to, service, seconds, fare; };
struct Query {
 int source=0, destination=0, objective=0, mode=0;
 std::vector<int> closedStations, closedEdges, closedServices;
 // Internal spur-search context used by Yen; not accepted from HTTP clients.
 int initialService=0;
 bool initialWalked=false;
};
struct Route {
 bool found=false;
 std::int64_t seconds=0, rideSeconds=0, walkSeconds=0, waitSeconds=0, fare=0;
 int transfers=0, expanded=0, relaxed=0;
 double micros=0;
 std::vector<int> stations, edges, waits;
};
class Engine {
 int n_, services_;
 std::vector<Edge> edges_;
 std::vector<std::vector<Edge>> adj_;
 std::vector<int> waits_;
public:
 Engine(int nodes, int services, std::vector<Edge> edges, std::vector<int> waits);
 Route route(const Query& q) const;
 std::vector<Route> alternatives(const Query& q, int k) const;
 Cost score(const Route& r, int objective) const;
private:
 Route account(const Query& q, const std::vector<int>& edgeIds) const;
};
}
