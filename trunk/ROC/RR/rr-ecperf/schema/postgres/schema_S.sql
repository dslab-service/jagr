
CREATE TABLE S_component
(
	comp_id			char(15) not null,
	comp_name		char(10),
	comp_desc		varchar(100),
	comp_unit		char(10),
	comp_cost		FLOAT8,
	qty_on_order		integer,
	qty_demanded		integer,
	lead_time		integer,
	container_size		integer
);

CREATE  INDEX S_comp_idx ON S_component (comp_id);

CREATE TABLE S_supp_component
(
	sc_p_id			char(15) not null,
	sc_supp_id		integer not null,
	sc_price		FLOAT8,
	sc_qty			integer,
	sc_discount		FLOAT8,
	sc_del_date		integer
);

CREATE  INDEX S_sc_idx ON S_supp_component (sc_p_id, sc_supp_id);

CREATE TABLE S_supplier
(
	supp_id			integer not null,
	supp_name		char(16),
	supp_street1	char(20),	
	supp_street2	char(20),	
	supp_city		char(20),	
	supp_state		char(2),	
	supp_country	char(10),	
	supp_zip		char(9),	
	supp_phone		char(16),
	supp_contact	char(25)
);

CREATE  INDEX S_supp_idx ON S_supplier (supp_id);

CREATE TABLE S_site
(
	site_id			integer not null,
	site_name		char(16),
	site_street1	char(20),	
	site_street2	char(20),	
	site_city		char(20),	
	site_state		char(2),	
	site_country	char(10),	
	site_zip		char(9)
);

CREATE  INDEX S_site_idx ON S_site (site_id);

CREATE TABLE S_purchase_order
(
	po_number		integer not null,
	po_supp_id		integer,
	po_site_id		integer
);

CREATE  INDEX S_po_idx ON S_purchase_order (po_number);

CREATE TABLE S_purchase_orderline
(
	pol_number		integer not null,
	pol_po_id		integer not null,
	pol_p_id		char(15),
	pol_qty			integer,
	pol_balance		FLOAT8,
	pol_deldate		DATE,
	pol_message		varchar(100)
);

CREATE  INDEX S_pol_idx ON S_purchase_orderline (pol_po_id, pol_number);

