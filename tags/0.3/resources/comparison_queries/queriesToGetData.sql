-- getting domain, secondary domain and resolved ips matching between java and python db
select 		cd.newerClusterId, cd.olderClusterId, cd.matching as domain_match, 
		csd.matching as secondary_domain_match, ip.matching as ip_match 
from 		compareTableClustersDomain cd 
inner join 	compareTableClusterResolvedIP ip on cd.newerClusterId = ip.newerClusterId
		and cd.olderClusterId = ip.olderClusterId
inner join 	compareTableClustersSecondaryDomain csd on csd.newerClusterId = cd.newerClusterId
		and csd.olderClusterId = cd.olderClusterId
where 		cd.matching >= 50
order by 	cd.newerClusterId


-- getting the clusters in java db which do not have overlap of more than 50 % with any cluster in python db
select 	distinct cluster_id 
from 	clusters_20120417 
where 	cluster_id not in 
	( 
		select 		distinct cd.newerClusterId 	
		from 		comparetableclusterdomains cd 
		inner join 	comparetableclusterresolvedip ip on cd.newerClusterId = ip.newerClusterId
				and cd.olderClusterId = ip.olderClusterId
		inner join 	comparetablecluster csd on csd.newerClusterId = cd.newerClusterId
				and csd.olderClusterId = cd.olderClusterId
		where cd.matching > 50
	)
order by cluster_id


-- getting the clusters in python db which do not have overlap of more than 50 % with any cluster in java db
select 	distinct cluster_id 
from 	public_old.clusters_20120417 
where 	cluster_id not in 
	( 
		select distinct cd.olderClusterId 
		from 		comparetableclusterdomains cd 
		inner join 	comparetableclusterresolvedip ip on cd.newerClusterId = ip.newerClusterId
				and cd.olderClusterId = ip.olderClusterId
		inner join 	comparetablecluster csd on csd.newerClusterId = cd.newerClusterId
				and csd.olderClusterId = cd.olderClusterId
		where 		cd.matching >= 50
	)
order by cluster_id