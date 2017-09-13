--CBD_TAX_TYPES_T is part of TAXBASE schema 5001 = (select seq_no_tt from cbd_tax_type_t where tax_type='DV')
update pa_tax_calc_rule_t set seq_no_cr='1001' where seq_no_ti = (select seq_no_ti from pa_tax_indicator_t where taxind='MAT_DOM') and seq_no_tt = '5001';
