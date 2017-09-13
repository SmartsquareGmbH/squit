delete from PA_GA_EXCEPTION_T where  SEQ_NO_GAX between 5000000 and 6000000;
update cbd_bu_main_address_t set sta_code='DENW' where bu_code='119' and bu_type='STO' and VALID_FROM=to_date('18-JUL-12','DD-MON-RR');
update cbd_bu_main_address_t set sta_code='DEHE' where bu_code='1206' and bu_type='COM' and VALID_FROM=to_date('03-APR-13','DD-MON-RR');
