#include <bits/stdc++.h>
using namespace std;
#define V 40
int adj_mat(int a[40][40])
{
    for(int i=0; i<13 ; i++)            //Blue line from central to airport
    {
        a[i][i+1]=2;
        a[i+1][i]=2;
    }
    a[0][14]=3;                         //Red line from central to egmore
    a[14][0]=3;
    for(int i=14; i<26 ; i++)           //Red line from egmore to tambaram
    {
        a[i][i+1]=3;
        a[i+1][i]=3;
    }
    a[19][20]=0;                        //removing saidapet and st.thomas mount egde
    a[20][19]=0;
    a[19][9]=3;                         //saidapet to guindy
    a[9][19]=3;
    a[9][20]=3;                         //guindy to st.thomas mount
    a[20][9]=3;
    a[22][23]=0;                        //remove meenambakam to pallavaram
    a[23][22]=0;
    a[22][13]=2;                        //meenambakam to airport (tirusulam)
    a[13][22]=2;
    a[13][23]=3;                        //airport to pallavaram
    a[23][13]=3;

    a[14][27]=2;                        //green line egmore to nehru park metro
    a[27][14]=2;
    for(int i=27; i<38 ; i++)           //green line nehru park to ekatuthangal
    {
        a[i][i+1]=2;
        a[i+1][i]=2;
    }
    a[33][34]=3;			//Thirumangalam to Koyambedu
    a[34][33]=3;
    a[38][39]=3;			//Ashoknagar to ekkatuthangal - 3 mins
    a[39][38]=3;
    a[39][10]=2;                        //ekatuthangal to alandur
    a[10][39]=2;
    a[10][20]=2;                        //alandur to St.Thomas mount
    a[20][10]=2;
    return 0;
}
void print_matrix(int a[40][40])	//my func
{
    for(int i=0; i<40 ; i++)            //Prints full matrix
    {
        for (int j=0; j<40 ; j++)
        {
            cout<<a[i][j]<<" ";
        }
        cout<<endl;
    }
}
int minDistance(int dist[], bool sptSet[])
{
	int min1 = INT_MAX, min_index;

	for (int v = 0; v < V; v++)
	{

		if (sptSet[v] == false && dist[v] < min1)
			{
			    min1 = dist[v];
                min_index = v;
		//cout<<v<<" "<<dist[v]<<"\n";
			}

	}
	return min_index;
}
void printpath(int parent[],int i,map<int,string>mp)
{
    if(parent[i]==-1)
        return ;

    printpath(parent,parent[i],mp);
       cout<<mp[i]<<"\n\n";

}
void printSolution(int dist[],int parent[],int des,map<int,string>mp,int src)
{
	for (int i = 0; i < V; i++)
	{
	    if(i==des)
		{
                cout << "\nTime Taken: "<<dist[i]<<" Minutes\n";
                cout<<"\nPath taken :\n\n";
         	cout<<mp[src]<<endl<<endl;
		printpath(parent,i,mp);
		//cout<<mp[des];
		}
	}
}
void dijkstra(int a[V][V],int src,int des,map<int,string>mp)
{
	int dist[V];
	bool sptSet[V];
	int parent[V];
	for (int i = 0; i < V; i++)
		{
		    dist[i] = INT_MAX;
		    sptSet[i] = false;
		}
	dist[src] = 0;
	parent[src]=-1;
	for (int count = 0; count < V - 1; count++) {
		int u = minDistance(dist, sptSet);
		sptSet[u] = true;

		for (int v = 0; v < V; v++)
			if (!sptSet[v] && a[u][v]>0 && dist[u] != INT_MAX
				&& dist[u] + a[u][v] < dist[v])
			{
			    dist[v] = dist[u] + a[u][v];
			    parent[v]=u;
	//		    cout<<"hi"<<u<<" "<<v<<"\n";

			}
	}
	printSolution(dist,parent,des,mp,src);
}
void mapped(map<int,string> &mp)  			//my func
{
    mp[0]="Chennai Central";
    mp[1]="Government Estate metro";
    mp[2]="LIC metro";
    mp[3]="Thousand lights metro";
    mp[4]="AG-DMS metro";
    mp[5]="Teynampet metro";
    mp[6]="Nandanam metro";
    mp[7]="Saidapet metro";
    mp[8]="Little mount metro";
    mp[9]="Guindy";
    mp[10]="Alandur metro";
    mp[11]="Nanganallur metro";
    mp[12]="Meenambakkam metro";
    mp[13]="Airport";
    mp[14]="Egmore";
    mp[15]="Chetpet";
    mp[16]="Nungambakkam";
    mp[17]="Kodambakkam";
    mp[18]="Mambalam";
    mp[19]="Saidapet";
    mp[20]="St.Thomas mount";
    mp[21]="Palavandhangal";
    mp[22]="Meenambakkam";
    mp[23]="Pallavaram";
    mp[24]="Chrompet";
    mp[25]="Tambaram Sanitorium";
    mp[26]="Tambaram";
    mp[27]="Nehru Park metro";
    mp[28]="Kilpauk metro";
    mp[29]="Pachaiappas College metro";
    mp[30]="Shenoy Nagar metro";
    mp[31]="Anna Nagar East metro";
    mp[32]="Anna Nagar Tower metro";
    mp[33]="Thirumangalam metro";
    mp[34]="Koyambedu metro";
    mp[35]="CMBT metro";
    mp[36]="Arumbakkam metro";
    mp[37]="Vadapalani metro";
    mp[38]="Ashok Nagar metro";
    mp[39]="Ekatuthangal metro";
    for(int i=0; i<40; i++)
    cout<<i+1<<"."<<mp[i]<<endl;
}
int main()
{   map<int,string>::iterator itr;
    int src,des,flag=0;
    cout<<"\n\nFINDING THE FASTEST ROUTE THROUGH METRO/RAILWAYS\n";
    cout<<"\n------------------------------------------------------------\n\n";
    int graph[40][40]={};
    adj_mat(graph);
    //print_matrix(graph);
    map <int,string> area;
    mapped(area);
    cout<<"\n------------------------------------------------------------\n";
    cout<<"\n Enter the boarding station number:";
    cin>>src;
    src=src-1;
    cout<<"\n Enter the destination station number:";
    cin>>des;
    des=des-1;
    cout<<"\n------------------------------------------------------------\n\n";
    if(area[src]=="")
    {
    cout<<"Oops! invalid Source";
    return -1;
    }
    if(area[des]=="")
    {
    cout<<"Oops! invalid Destination";
    return -1;
    }
    dijkstra(graph,src,des,area);
    //for(int i=0; i<40; i++)
    //cout<<i+1<<"."<<area[i]<<endl;
    return 0;
}

