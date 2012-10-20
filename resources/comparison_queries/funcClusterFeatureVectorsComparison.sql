CREATE OR REPLACE FUNCTION funcClusterFeatureVectorsComparison(newerDate DATE, olderDate DATE) RETURNS integer AS $$
  DECLARE
    clusterId Integer = 0;
    networkCardinality Real = 0;
    ipDiversity Real = 0;
    domainsPerNetwork Real = 0;
    numberOfDomains Real = 0;
    ttlPerDomain Real = 0;
    ipGrowthRatio Real = 0;
    queriesPerDomain Real = 0;
    avgLastGrowthRatioSingleEntry Real = 0;
    avgLastGrowthRatioEntries Real = 0;
    avgLastGrowthPrefixRatioEntries Real = 0;
    lastGrowthRatioCluster Real = 0;
    lastGrowthPrefixRatioCluster Real = 0;
    lastGrowthRatioPrevClusters Real = 0;
    lastGrowthPrefixRatioPrevClusters Real = 0;
    noveltyOneWeek Real = 0;
    rowCount Real = 0;
  BEGIN

	IF EXISTS
	(
		SELECT 	* 
		FROM 	information_schema.tables 
		WHERE 	table_catalog = CURRENT_CATALOG 
			AND table_schema = CURRENT_SCHEMA
			AND table_name = 'comparetableclusterfeaturevector'
	) THEN

		DROP TABLE compareTableClusterFeatureVector;
	END IF;

	create table compareTableClusterFeatureVector (
		new_cluster_id int null, 
		old_cluster_id int not null, 
		network_cardinality_diff real null, 
		ip_diversity_diff real null, 
		domains_per_network_diff real null,
		number_of_domains_diff real null,
		ttl_per_domain_diff real null,
		ip_growth_ratio_diff real null,
		queries_per_domain_diff real null,
		avg_last_growth_ratio_single_entry_diff real null,
		avg_last_growth_ratio_entries_diff real null,
		avg_last_growth_prefix_ratio_entries_diff real null,
		last_growth_ratio_cluster_diff real null,
		last_growth_prefix_ratio_cluster_diff real null,
		last_growth_ratio_prev_clusters_diff real null,
		last_growth_prefix_ratio_prev_clusters_diff real null,
		novelty_one_week_diff real null
		);
	

	SELECT count (*) into rowCount
	from public.compareTableClustersDomain; 

	WHILE rowCount>0 LOOP
		
		--SELECT * FROM cluster_feature_vecor
	      
		INSERT INTO compareTableClusterFeatureVector (
			SELECT 	n.cluster_id, o.cluster_id,
				n.network_cardinality - o.network_cardinality,
				n.ip_diversity - o.ip_diversity,
				n.domains_per_network - o.domains_per_network,
				n.number_of_domains - o.number_of_domains,
				n.ttl_per_domain - o.ttl_per_domain,
				n.ip_growth_ratio - o.ip_growth_ratio,
				n.queries_per_domain - o.queries_per_domain,
				n.avg_last_growth_ratio_single_entry - o.avg_last_growth_ratio_single_entry,
				n.avg_last_growth_ratio_entries - o.avg_last_growth_ratio_entries,
				n.avg_last_growth_prefix_ratio_entries - o.avg_last_growth_prefix_ratio_entries,
				n.last_growth_ratio_cluster - o.last_growth_ratio_cluster,
				n.last_growth_prefix_ratio_cluster - o.last_growth_prefix_ratio_cluster,
				n.last_growth_ratio_prev_clusters - o.last_growth_ratio_prev_clusters,
				n.last_growth_prefix_ratio_prev_clusters - o.last_growth_prefix_ratio_prev_clusters,
				n.novelty_one_week - o.novelty_one_week
			FROM 
				public.cluster_feature_vectors AS n
					JOIN
				public_old.cluster_feature_vectors AS o
					ON 
				n.cluster_id = (select newerClusterId from compareTableClustersDomain limit 1 offset rowCount-1) AND
				o.cluster_id = (select olderClusterId from compareTableClustersDomain limit 1 offset rowCount-1)
			WHERE
				n.log_date = newerDate AND
				o.log_date = olderDate
		);

		rowCount := rowCount - 1;
	END LOOP;
    
RETURN rowCount;
END;$$
LANGUAGE 'plpgsql';

/*
select * from funcClusterFeatureVectorsComparison('20120417', '20120417');
select * from compareTableClusterFeatureVector;
*/