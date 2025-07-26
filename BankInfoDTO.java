package model;

import com.google.gson.annotations.SerializedName;

public class BankInfoDTO {
    @SerializedName("bankName")
    private String bankName;
    @SerializedName("accountNumber")
    private String accountNumber;

    public BankInfoDTO(String bankName, String accountNumber) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
    }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
}