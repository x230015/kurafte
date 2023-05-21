/*
The purpose of this file is for a better emulation and testing of the Kurafte Token.
It mimics the Solidity main contract, and is capable of emulate all of the token functions.
*/

package tests.token;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class Kurafte {

    private String symbol;
    private String name;
    private byte decimals;
    private BigInteger _DECIMALSCONSTANT;
    private BigInteger _totalSupply;
    private BigInteger _currentSupply;
    private boolean constructorLocked = false;
    private Map<String, BigInteger> balances = new HashMap<>();
    private Map<String, Map<String, BigInteger>> allowed = new HashMap<>();
    private BigInteger nativeDeposited;

    private BigInteger div;
    private BigInteger fee = BigInteger.valueOf(50); // 2% since 0.02 = 2/100 = 1/50



    //--------for java only helpers
    private String owner;
    private String zero = "0";

    private String msgSender;
    private BigInteger msgSenderValue;
    public void setMsgSender(String address){
        this.msgSender = address;
    }

    public void setMsgSenderValue(BigInteger msgSenderValue) {
        this.msgSenderValue = msgSenderValue;
    }
    //--------end for java only helpers

    public Kurafte() {
        if (constructorLocked) {
            throw new RuntimeException();
        }
        constructorLocked = true; // a bullet-proof mechanism

        symbol = "KURAFTE";
        name = "KURAFTE";
        decimals = 18;
        _DECIMALSCONSTANT = BigInteger.TEN.pow(decimals);
        _totalSupply = BigInteger.valueOf(21000).multiply(_DECIMALSCONSTANT);
        _currentSupply = BigInteger.ONE;
        nativeDeposited = BigInteger.ONE;
        div = BigInteger.valueOf(5250000);

        // We will transfer the ownership only once, making sure there is no owner.
        owner = zero;
    }


    public BigInteger totalSupply() {
        return _totalSupply;
    }

    public BigInteger currentSupply() {
        return _currentSupply;
    }

    public BigInteger balanceOf(String tokenOwner) {
        return balances.getOrDefault(tokenOwner, BigInteger.ZERO);
    }



    private void transfer(String to, BigInteger tokens) {
        if (to.equals("0")) {return;}
        String from = msgSender;
        balances.put(from, balances.get(from).subtract(tokens));
        balances.put(to, balances.getOrDefault(to, BigInteger.ZERO).add(tokens));
    }


    public boolean approve(String spender, BigInteger tokens) {
        String from = msgSender;
        allowed.get(from).put(spender, tokens);
        return true;
    }

    private void transferFrom(String from, String to, BigInteger tokens) {
        if (to.equals("0")){return;}
        if (from.equals("0")){return;}
        balances.put(from, balances.get(from).subtract(tokens));
        allowed.get(from).put(msgSender, allowed.get(from).get(msgSender).subtract(tokens));
        balances.put(to, balances.getOrDefault(to, BigInteger.ZERO).add(tokens));
    }

    public BigInteger allowance(String tokenOwner, String spender) {
        return allowed.get(tokenOwner).getOrDefault(spender, BigInteger.ZERO);
    }

    public boolean approveAndCall(String spender, BigInteger tokens) {
        allowed.get(msgSender).put(spender, tokens);
        return true;
    }


    public BigInteger mine() {

        BigInteger originalDeposit = msgSenderValue;
        if(originalDeposit.compareTo(BigInteger.ZERO) <= 0){return BigInteger.ZERO;}

        BigInteger allowedDeposit = getAllowedNative();
        if(allowedDeposit.compareTo(BigInteger.ZERO) <= 0){return BigInteger.ZERO;}

        BigInteger toConvert = originalDeposit.compareTo(allowedDeposit) <= 0 ? originalDeposit : allowedDeposit;
        if(toConvert.compareTo(BigInteger.ZERO) <= 0){return BigInteger.ZERO;}

        BigInteger tokens = toTokens(toConvert);
        if(tokens.compareTo(BigInteger.ZERO) <= 0) {return BigInteger.ZERO;}

        // Mint new tokens
        BigInteger balance = balances.get(msgSender)==null?BigInteger.ZERO:balances.get(msgSender);
        balances.put(msgSender, balance.add(tokens));
        _currentSupply = _currentSupply.add(tokens);

        // Refund the excess
        BigInteger refund = originalDeposit.subtract(toConvert);
        nativeDeposited = nativeDeposited.add(toConvert);

        if (refund.compareTo(BigInteger.ZERO) > 0) {
            return refund;
        }

        return BigInteger.ZERO; //this is a refund, for balance, access function
    }

    public boolean swap(BigInteger tokens) {
        if(tokens.compareTo(BigInteger.ZERO) <= 0){return false;}

        BigInteger allowedMax = getAllowedTokens();
        if (tokens.compareTo(allowedMax) > 0) {
            tokens = allowedMax;
        }
        BigInteger nativeAmount = toNative(tokens);
        if (nativeAmount.compareTo(BigInteger.ZERO) <= 0) {
            return false;
        }

        // Burn tokens to get wei
        balances.put(msgSender, balances.get(msgSender).subtract(tokens));
        _currentSupply = _currentSupply.subtract(tokens);

        if (nativeAmount.compareTo(BigInteger.ZERO) <= 0) {
            return false;
        }
        nativeDeposited = nativeDeposited.subtract(nativeAmount);
        return true;  //for balance, access function

    }

    public BigInteger toTokens(BigInteger nativeAmount) {
        BigInteger numerator = nativeAmount.multiply(_currentSupply).multiply(fee.subtract(BigInteger.ONE));
        BigInteger denominator = nativeDeposited.multiply(fee);
        return numerator.divide(denominator);
    }

    public BigInteger toNative(BigInteger tokens) {
        BigInteger numerator = nativeDeposited.multiply(tokens).multiply(fee.subtract(BigInteger.ONE));
        BigInteger denominator = _currentSupply.multiply(fee);
        return numerator.divide(denominator);
    }

    public BigInteger getAllowedTokens() {
        return (_totalSupply.subtract(_currentSupply)).divide(div);
    }

    public BigInteger getAllowedNative() {
        return toNative(getAllowedTokens());
    }


}
