Update CBD_TAX_PARAM_T set delete_date = sysdate where TAX_TYPE = 'POSTP';
Insert into CBD_TAX_PARAM_T (SEQ_NO_TAXP,TAX_TYPE,DESCRIPTION,DEF_VALUE,VALID_FROM,VALID_TO,IDRS_IMPORT_DATE,DELETE_DATE) 
values (555555555,'POSTP','param for test','Y',to_date('01-SEP-04','DD-MON-RR'),to_date('16-SEP-15','DD-MON-RR'),to_date('18-SEP-15','DD-MON-RR'),null);
