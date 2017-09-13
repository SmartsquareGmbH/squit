update CBD_GEOGRAPHICAL_AREA_T set valid_to= to_date('15-FEB-13','DD-MON-RR') where seq_no_ga=48732;
update cbd_ga_taxtypes_t set valid_to=to_date('15-FEB-13','DD-MON-RR') where seq_no_gtt=2889;
Insert into CBD_GEOGRAPHICAL_AREA_T (SEQ_NO_GA,GA_CODE,GA_TYPE,NAME,CUR_CODE,VALID_FROM,VALID_TO,IDRS_IMPORT_DATE,DELETE_DATE) values (5000001,'CA','CTY','CANADA','TST',to_date('15-FEB-13','DD-MON-RR'),null,to_date('11-FEB-15','DD-MON-RR'),null);
Insert into CBD_GA_TAXTYPES_T (SEQ_NO_GTT,SEQ_NO_GA,TAX_TYPE,TAX_TYPE_DESCR,TAX_CODE,VALID_FROM,VALID_TO,IDRS_IMPORT_DATE,DELETE_DATE) values (5000002,5000001,'GST','TEST','0',to_date('20-JAN-13','DD-MON-RR'),null,to_date('04-APR-13','DD-MON-RR'),null);
