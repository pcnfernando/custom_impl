package org.wso2.extension.siddhi.execution.bny.bean;
/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 *
 */
public class Expression implements Comparable<Expression> {

    private String ruleMstrId;
    private String operCd;
    private String varlNm;
    private String varlOperCd;
    private String varlValTx;
    private String varlTyCd;
    private int ruleSeqNr;

    public Expression(String ruleMstrId, String varlNm, String varlTyCd, String varlOperCd, String varlValTx,
                      String operCd,  int ruleSeqNr) {
        this.ruleMstrId = ruleMstrId;
        this.varlNm = varlNm;
        this.varlTyCd = varlTyCd;
        this.varlOperCd = varlOperCd;
        this.varlValTx = varlValTx;
        this.operCd = operCd;
        this.ruleSeqNr = ruleSeqNr;
    }

    public String getRuleMstrId() {
        return ruleMstrId;
    }

    public void setRuleMstrId(String ruleMstrId) {
        this.ruleMstrId = ruleMstrId;
    }

    public String getOperCd() {
        return operCd;
    }

    public void setOperCd(String operCd) {
        this.operCd = operCd;
    }

    public String getVarlNm() {
        return varlNm;
    }

    public void setVarlNm(String varlNm) {
        this.varlNm = varlNm;
    }

    public String getVarlOperCd() {
        return varlOperCd;
    }

    public void setVarlOperCd(String varlOperCd) {
        this.varlOperCd = varlOperCd;
    }

    public String getVarlValTx() {
        return varlValTx;
    }

    public void setVarlValTx(String varlValTx) {
        this.varlValTx = varlValTx;
    }

    public String getVarlTyCd() {
        return varlTyCd;
    }

    public void setVarlTyCd(String varlTyCd) {
        this.varlTyCd = varlTyCd;
    }

    public int getRuleSeqNr() {
        return ruleSeqNr;
    }

    public void setRuleSeqNr(int ruleSeqNr) {
        this.ruleSeqNr = ruleSeqNr;
    }

    @Override
    public int compareTo(Expression o) {
        return this.getRuleSeqNr() - o.getRuleSeqNr();
    }

    @Override
    public int hashCode() {
        int result = ruleMstrId != null ? ruleMstrId.hashCode() : 0;
        result = 31 * result + ruleSeqNr;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }
        /* Check if o is an instance of Complex or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof Expression)) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        // typecast o to Complex so that we can compare data members
        Expression c = (Expression) o;
        if (this == o) {
            return true;
        }
        if (ruleSeqNr != (c.ruleSeqNr)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("RuleMasterId: %s, VariableName: %s, Operator:%s, Value:%s \n", ruleMstrId, varlNm, operCd,
                varlValTx);
    }
}
