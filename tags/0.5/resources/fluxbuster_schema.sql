--
-- PostgreSQL database dump
--

-- Dumped from database version 8.4.11
-- Dumped by pg_dump version 9.1.3

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 196 (class 1259 OID 27988)
-- Dependencies: 6
-- Name: cluster_classes; Type: TABLE; Schema: public; Owner: ; Tablespace: 
--

CREATE TABLE cluster_classes (
    cluster_id integer NOT NULL,
    sensor_name character(3) NOT NULL,
    log_date date NOT NULL,
    class character varying(10) NOT NULL,
    validated boolean NOT NULL
);


--
-- TOC entry 200 (class 1259 OID 36231)
-- Dependencies: 6
-- Name: cluster_domainname_similarity; Type: TABLE; Schema: public; Owner: ; Tablespace: 
--

CREATE TABLE cluster_domainname_similarity (
    cluster_id integer NOT NULL,
    candidate_cluster_id integer NOT NULL,
    similarity real NOT NULL,
    log_date date NOT NULL,
    candidate_log_date date NOT NULL
);


--
-- TOC entry 2005 (class 0 OID 0)
-- Dependencies: 200
-- Name: COLUMN cluster_domainname_similarity.similarity; Type: COMMENT; Schema: public; Owner: buster
--

COMMENT ON COLUMN cluster_domainname_similarity.similarity IS 'Size of the intersection over the size of <cluster_id>';


--
-- TOC entry 140 (class 1259 OID 27438)
-- Dependencies: 6
-- Name: cluster_feature_vectors; Type: TABLE; Schema: public; Owner: ; Tablespace: 
--

CREATE TABLE cluster_feature_vectors (
    cluster_id integer NOT NULL,
    sensor_name character(3) NOT NULL,
    log_date date NOT NULL,
    network_cardinality real NOT NULL,
    ip_diversity real NOT NULL,
    domains_per_network real,
    number_of_domains real NOT NULL,
    ttl_per_domain real NOT NULL,
    ip_growth_ratio real NOT NULL,
    queries_per_domain real NOT NULL,
    avg_last_growth_ratio_single_entry real,
    avg_last_growth_ratio_entries real,
    avg_last_growth_prefix_ratio_entries real,
    last_growth_ratio_cluster real,
    last_growth_prefix_ratio_cluster real,
    last_growth_ratio_prev_clusters real,
    last_growth_prefix_ratio_prev_clusters real,
    novelty_six_months real,
    novelty_one_month real,
    novelty_one_week real
);


--
-- TOC entry 198 (class 1259 OID 36220)
-- Dependencies: 6
-- Name: cluster_ip_similarity; Type: TABLE; Schema: public; Owner: ; Tablespace: 
--

CREATE TABLE cluster_ip_similarity (
    cluster_id integer NOT NULL,
    candidate_cluster_id integer NOT NULL,
    similarity real NOT NULL,
    log_date date NOT NULL,
    candidate_log_date date NOT NULL
);


--
-- TOC entry 2006 (class 0 OID 0)
-- Dependencies: 198
-- Name: COLUMN cluster_ip_similarity.similarity; Type: COMMENT; Schema: public; Owner: buster
--

COMMENT ON COLUMN cluster_ip_similarity.similarity IS 'Size of the intersection over the size of <cluster_id>';


--
-- TOC entry 151 (class 1259 OID 27481)
-- Dependencies: 6
-- Name: cluster_resolved_ips; Type: TABLE; Schema: public; Owner: ; Tablespace: 
--

CREATE TABLE cluster_resolved_ips (
    cluster_id integer NOT NULL,
    sensor_name character(3) NOT NULL,
    log_date date NOT NULL,
    resolved_ip inet NOT NULL
);


--
-- TOC entry 162 (class 1259 OID 27557)
-- Dependencies: 6
-- Name: clusters; Type: TABLE; Schema: public; Owner: ; Tablespace: 
--

CREATE TABLE clusters (
    cluster_id integer NOT NULL,
    domain_id integer NOT NULL,
    sensor_name character(3) NOT NULL,
    log_date date NOT NULL
);


--
-- TOC entry 173 (class 1259 OID 27600)
-- Dependencies: 1983 6
-- Name: domains; Type: TABLE; Schema: public; Owner: ; Tablespace: 
--

CREATE TABLE domains (
    domain_id integer NOT NULL,
    domain_name character varying(1024) NOT NULL,
    log_date date NOT NULL,
    second_level_domain_name character varying(128) DEFAULT 'undefined'::character varying NOT NULL
);


--
-- TOC entry 184 (class 1259 OID 27687)
-- Dependencies: 173 6
-- Name: domains_domain_id_seq; Type: SEQUENCE; Schema: public; Owner: buster
--

CREATE SEQUENCE domains_domain_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- TOC entry 2007 (class 0 OID 0)
-- Dependencies: 184
-- Name: domains_domain_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: buster
--

ALTER SEQUENCE domains_domain_id_seq OWNED BY domains.domain_id;


--
-- TOC entry 185 (class 1259 OID 27689)
-- Dependencies: 6
-- Name: resolved_ips; Type: TABLE; Schema: public; Owner: ; Tablespace: 
--

CREATE TABLE resolved_ips (
    domain_id integer NOT NULL,
    log_date date NOT NULL,
    resolved_ip inet NOT NULL
);

--
-- TOC entry 1984 (class 2604 OID 27765)
-- Dependencies: 184 173
-- Name: domain_id; Type: DEFAULT; Schema: public; Owner: buster
--

ALTER TABLE ONLY domains ALTER COLUMN domain_id SET DEFAULT nextval('domains_domain_id_seq'::regclass);


--
-- TOC entry 1998 (class 2606 OID 27992)
-- Dependencies: 196 196 196 196
-- Name: cluster_classes_pkey; Type: CONSTRAINT; Schema: public; Owner: ; Tablespace: 
--

ALTER TABLE ONLY cluster_classes
    ADD CONSTRAINT cluster_classes_pkey PRIMARY KEY (cluster_id, sensor_name, log_date);


--
-- TOC entry 2002 (class 2606 OID 36235)
-- Dependencies: 200 200 200 200 200 200
-- Name: cluster_domainname_similarity_pkey; Type: CONSTRAINT; Schema: public; Owner: ; Tablespace: 
--

ALTER TABLE ONLY cluster_domainname_similarity
    ADD CONSTRAINT cluster_domainname_similarity_pkey PRIMARY KEY (cluster_id, candidate_cluster_id, log_date, candidate_log_date, similarity);


--
-- TOC entry 1986 (class 2606 OID 27845)
-- Dependencies: 140 140 140 140
-- Name: cluster_feature_vectors_pkey; Type: CONSTRAINT; Schema: public; Owner: ; Tablespace: 
--

ALTER TABLE ONLY cluster_feature_vectors
    ADD CONSTRAINT cluster_feature_vectors_pkey PRIMARY KEY (cluster_id, sensor_name, log_date);


--
-- TOC entry 2000 (class 2606 OID 36224)
-- Dependencies: 198 198 198 198 198 198
-- Name: cluster_ip_similarity_pkey; Type: CONSTRAINT; Schema: public; Owner: ; Tablespace: 
--

ALTER TABLE ONLY cluster_ip_similarity
    ADD CONSTRAINT cluster_ip_similarity_pkey PRIMARY KEY (cluster_id, candidate_cluster_id, log_date, candidate_log_date, similarity);


--
-- TOC entry 1990 (class 2606 OID 27847)
-- Dependencies: 162 162 162
-- Name: cluster_pkey; Type: CONSTRAINT; Schema: public; Owner: ; Tablespace: 
--

ALTER TABLE ONLY clusters
    ADD CONSTRAINT cluster_pkey PRIMARY KEY (domain_id, sensor_name);


--
-- TOC entry 1988 (class 2606 OID 27869)
-- Dependencies: 151 151 151 151 151
-- Name: cluster_resolved_ips_pkey; Type: CONSTRAINT; Schema: public; Owner: ; Tablespace: 
--

ALTER TABLE ONLY cluster_resolved_ips
    ADD CONSTRAINT cluster_resolved_ips_pkey PRIMARY KEY (cluster_id, sensor_name, log_date, resolved_ip);


--
-- TOC entry 1992 (class 2606 OID 27913)
-- Dependencies: 173 173
-- Name: domains_domain_name_key; Type: CONSTRAINT; Schema: public; Owner: ; Tablespace: 
--

ALTER TABLE ONLY domains
    ADD CONSTRAINT domains_domain_name_key UNIQUE (domain_name);


--
-- TOC entry 1994 (class 2606 OID 27915)
-- Dependencies: 173 173
-- Name: domains_pkey; Type: CONSTRAINT; Schema: public; Owner: ; Tablespace: 
--

ALTER TABLE ONLY domains
    ADD CONSTRAINT domains_pkey PRIMARY KEY (domain_id);


--
-- TOC entry 1996 (class 2606 OID 27937)
-- Dependencies: 185 185 185 185
-- Name: resolved_ips_pkey; Type: CONSTRAINT; Schema: public; Owner: ; Tablespace: 
--

ALTER TABLE ONLY resolved_ips
    ADD CONSTRAINT resolved_ips_pkey PRIMARY KEY (domain_id, log_date, resolved_ip);

--
-- PostgreSQL database dump complete
--

