CREATE OR REPLACE FUNCTION funcClustersComparisonSecondaryDomain(newerDate DATE, olderDate DATE) RETURNS INTEGER AS $$
DECLARE
	totalNewClustersIter INTEGER = 0;
	totalOldClustersIter INTEGER = 0;

	totalOldClusters INTEGER = 0;

	countDomainsPerCluster INTEGER = 0;
	countDomainsPerClusterOld INTEGER = 0;
	overlappingDomainCount INTEGER = 0;
    
BEGIN
	IF EXISTS
	(
		SELECT 	* 
		FROM 	information_schema.tables 
		WHERE 	table_catalog = CURRENT_CATALOG 
			AND table_schema = CURRENT_SCHEMA
			AND table_name = 'comparetableclusterssecondarydomain'
	) THEN

		DROP TABLE compareTableClustersSecondaryDomain;
	END IF;

	CREATE TABLE compareTableClustersSecondaryDomain
	(
		newerClusterId 		INTEGER NULL, 
		olderClusterId 		INTEGER NOT NULL, 
		newerClusterDomainCount	INTEGER NULL, 
		olderClusterDomainCount	INTEGER NULL, 
		overlappingDomainCount	INTEGER NULL,
		matching 		DECIMAL NULL
	);

	-- Get the number of cluster records on the new database table.
	SELECT 	count (DISTINCT cluster_id) 
	INTO 	totalNewClustersIter
	FROM 	public.clusters 
	WHERE 	log_date = newerDate;

	--Get the number of cluster records on the old fluxbuster database
	SELECT 	count (DISTINCT cluster_id) 
	INTO 	totalOldClusters
	FROM 	public_old.clusters 
	WHERE 	log_date = olderDate;

	CREATE TABLE temp_clusters_secondary_domain_reversed (cluster_id INTEGER, domain_name varchar(400));
	INSERT INTO temp_clusters_secondary_domain_reversed 
	(
		SELECT 		c.cluster_id, reverseDomain(d.second_level_domain_name) 
		FROM 		public.clusters c 
		INNER JOIN 	public.domains d ON c.domain_id = d.domain_id
		WHERE		c.log_date = newerDate
				AND d.log_date = newerDate
	);
		
	CREATE TABLE temp_clusters_secondary_domain_reversed_old (cluster_id INTEGER, domain_name varchar(400));
	INSERT INTO temp_clusters_secondary_domain_reversed_old 
	(
		SELECT 		c.cluster_id, d.second_level_domain_name 
		FROM 		public_old.clusters c 
		INNER JOIN 	public_old.domains d ON c.domain_id = d.domain_id
		WHERE		c.log_date = olderDate
				AND d.log_date = olderDate
	);
	
	WHILE totalNewClustersIter > 0 LOOP

		--Get the number of domains for a particular cluster in the new database
		SELECT 	count(*) 
		INTO 	countDomainsPerCluster 
		FROM 	public.clusters 
		WHERE 	cluster_id = totalNewClustersIter  
			AND log_date = newerDate;

		IF countDomainsPerCluster <> 0 THEN

			totalOldClustersIter = totalOldClusters;

			WHILE totalOldClustersIter > 0 LOOP

				--Get the number of domains for a particular cluster in the old database.
				SELECT 	count(*)
				INTO 	countDomainsPerClusterOld
				FROM 	public_old.clusters 
				WHERE 	cluster_id = totalOldClustersIter 
					AND log_date = olderDate;

				IF countDomainsPerClusterOld <> 0 THEN

					--Get the intersection of the records comparing domains of each cluster
					--in the old database to the cluster in the new database
					SELECT count(*) into overlappingDomainCount 
					FROM (
						SELECT 	domain_name 
						FROM 	temp_clusters_secondary_domain_reversed
						WHERE 	cluster_id = totalNewClustersIter

						INTERSECT 

						SELECT 	domain_name 
						FROM 	temp_clusters_secondary_domain_reversed_old
						WHERE 	cluster_id = totalOldClustersIter

					) AS temp_table;

					
					INSERT INTO compareTableClustersSecondaryDomain 
					VALUES
					(	
						totalNewClustersIter, 
						totalOldClustersIter, 
						countDomainsPerCluster, 
						countDomainsPerClusterOld, 
						overlappingDomainCount,
						overlappingDomainCount*100/ countDomainsPerCluster
					);

					totalOldClustersIter := totalOldClustersIter - 1;
				END IF;
			END LOOP;

			totalNewClustersIter := totalNewClustersIter - 1;
		END IF;
	END LOOP;

	DROP TABLE temp_clusters_secondary_domain_reversed;
	DROP TABLE temp_clusters_secondary_domain_reversed_old;
	
RETURN totalNewClustersIter;
END;$$
LANGUAGE 'plpgsql';


-- SELECT * from funcClustersComparisonSecondaryDomain('20120417', '20120417');