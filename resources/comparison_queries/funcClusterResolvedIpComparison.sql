CREATE OR REPLACE FUNCTION funcClusterResolvedIPsComparison(newerDate DATE, olderDate DATE) RETURNS integer AS $$
DECLARE
	totalNewClustersIter INTEGER = 0;
	totalOldClustersIter INTEGER = 0;

	totalOldClusters INTEGER = 0;

	
	countIPsPerCluster INTEGER = 0;
	countIPsPerClusterOld INTEGER = 0;
	
	overlappingIPsCount INTEGER = 0;
BEGIN
	IF EXISTS
	(
		SELECT 	* 
		FROM 	information_schema.tables 
		WHERE 	table_catalog = CURRENT_CATALOG 
			AND table_schema = CURRENT_SCHEMA
			AND table_name = 'comparetableclusterresolvedip'
	) THEN
		
		DROP TABLE compareTableClusterResolvedIP;
	END IF;

	CREATE TABLE compareTableClusterResolvedIP
	(
		newerClusterId 		INTEGER NULL, 
		olderClusterId 		INTEGER NOT NULL, 
		newerResolvedIPsCount	INTEGER NULL, 
		olderResolvedIPsCount	INTEGER NULL, 
		overlappingIPsCount	INTEGER NULL,
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

	WHILE totalNewClustersIter > 0 LOOP
		
		--Get the number of domains for a particular cluster in the new database
		SELECT 	count(*) 
		INTO 	countIPsPerCluster 
		FROM 	public.cluster_resolved_ips 
		WHERE 	cluster_id = totalNewClustersIter  
			AND log_date = newerDate;
		
		IF countIPsPerCluster <> 0 THEN
			
			totalOldClustersIter = totalOldClusters;

			WHILE totalOldClustersIter > 0 LOOP

				--Get the number of domains for a particular cluster in the older table.
				SELECT 	count(*)
				INTO 	countIPsPerClusterOld
				FROM 	public_old.cluster_resolved_ips 
				WHERE 	cluster_id = totalOldClustersIter 
					AND log_date = olderDate;

				IF countIPsPerClusterOld <> 0 THEN

					SELECT 	count(*) 
					INTO 	overlappingIPsCount 
					FROM 
					(
						SELECT 	resolved_ip
						FROM 	public.cluster_resolved_ips
						WHERE 	cluster_id = totalNewClustersIter
							AND log_date = newerDate

						INTERSECT

						SELECT 	resolved_ip
						FROM 	public_old.cluster_resolved_ips
						WHERE 	cluster_id = totalOldClustersIter
							AND log_date = olderDate
							
					) AS temp_table;

					INSERT INTO compareTableClusterResolvedIP 
					VALUES
					(
						totalNewClustersIter, 
						totalOldClustersIter, 
						countIPsPerCluster, 
						countIPsPerClusterOld, 
						overlappingIPsCount,
						overlappingIPsCount*100/ ((countIPsPerCluster + countIPsPerClusterOld)/2)
					);

					totalOldClustersIter := totalOldClustersIter - 1;
				END IF;
			END LOOP;
			totalNewClustersIter := totalNewClustersIter - 1;
		END IF;
	END LOOP;
    
RETURN 0;
END;$$
LANGUAGE 'plpgsql';

/*
select * from funcClusterResolvedIPsComparison('20120417', '20120417');
select * from compareTableClusterResolvedIP where matching >= 50 order by newerClusterId ;
*/