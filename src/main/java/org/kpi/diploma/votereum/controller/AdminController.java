package org.kpi.diploma.votereum.controller;

import org.kpi.diploma.votereum.entity.Ballot;
import org.kpi.diploma.votereum.service.BallotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final List<byte[]> candidates = new ArrayList<>();
    protected static BigInteger GAS_PRICE, GAS_LIMIT;
    private String PRIVATE_KEY;
    private String CONTRACT_ADDRESS;

    @Autowired
    private BallotService ballotService;

    @GetMapping("/panel")
    public String showAdminPanel(Model model) {
        model.addAttribute("candidates", convertByteArrayToString(candidates));
        return "admin-panel";
    }

    @PostMapping("/deploy")
    public String deploy(String candidatesInput, String privateKey, String gasPrice, String gasLimit) {
        PRIVATE_KEY = privateKey;
        GAS_LIMIT = new BigInteger(gasLimit);
        GAS_PRICE = new BigInteger(gasPrice);
        candidates.clear();
        candidates.addAll(convertToBytes32List(candidatesInput));
        Credentials credentials = Credentials.create(PRIVATE_KEY);
        Web3j web3j = Web3j.build(new HttpService());
        try {
            Ballot ballot = deployContract(web3j, credentials);
            ballotService.setDeployedBallot(ballot);
            CONTRACT_ADDRESS = ballotService.getDeployedBallot().getContractAddress();
            System.out.println("Contract Deployed at address: " + CONTRACT_ADDRESS);
            System.out.println("Inform this to every voter.");
        } catch (Exception ex) {
            Logger.getLogger(AdminController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "redirect:/manage-election/main";
    }

    private List<byte[]> convertToBytes32List(String candidatesInput) {
        List<byte[]> byteCandidates = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(candidatesInput, ",");
        while (tokenizer.hasMoreTokens()) {
            byteCandidates.add(stringToBytes32(tokenizer.nextToken()));
        }
        return byteCandidates;
    }
    public byte[] stringToBytes32(String string) {
        byte[] byteValue = string.getBytes();
        byte[] byteValueLen32 = new byte[32];
        System.arraycopy(byteValue, 0, byteValueLen32, 0, Math.min(byteValue.length, 32));
        return byteValueLen32;
    }

    private String convertByteArrayToString(List<byte[]> bytesList) {
        List<String> stringList = new ArrayList<>();
        for (byte[] bytes : bytesList) {
            stringList.add(new String(bytes).trim());
        }
        return String.join(",", stringList);
    }

    private Ballot deployContract(Web3j web3j, Credentials credentials) throws Exception {
        return Ballot.deploy(web3j, credentials, GAS_PRICE, GAS_LIMIT, candidates).send();
    }
}
