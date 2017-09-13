update PA_TAX_COMMENT_ML_T set DEFAUL_LANG='Y' where SEQ_NO_TC='1022' and ISO_LANG_CODE='fr' and ISO_CTY_CODE='FR' and TAX_COMMENT='Autoliquidation';
update PA_TAX_COMMENT_ML_T set DEFAUL_LANG='Y', VALID_FROM=to_date('15-MAR-15','DD-MON-RR') where SEQ_NO_TC='1022' and ISO_LANG_CODE='en' and ISO_CTY_CODE='GB' and TAX_COMMENT='Autoliquidation';
update PA_TAX_CALC_RULE_T set SEQ_NO_CR='1003' where SEQ_NO_TI='1051' and SEQ_NO_TT='5001';
