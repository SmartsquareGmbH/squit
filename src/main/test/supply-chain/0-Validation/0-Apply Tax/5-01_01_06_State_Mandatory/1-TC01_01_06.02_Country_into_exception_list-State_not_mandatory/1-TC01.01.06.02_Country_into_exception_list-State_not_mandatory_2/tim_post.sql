delete from PA_GA_EXCEPTION_T where  SEQ_NO_GAX between 5000000 and 6000000;
update cbd_bu_main_address_t set sta_code=null where bu_code='5100' and bu_type='COM' and VALID_FROM=to_date('01-JUN-08','DD-MON-RR');
