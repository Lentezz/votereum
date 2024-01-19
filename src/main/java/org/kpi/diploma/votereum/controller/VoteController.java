package org.kpi.diploma.votereum.controller;

import org.kpi.diploma.votereum.service.BallotService;
import org.kpi.diploma.votereum.entity.Ballot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import javax.swing.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

@Controller
@RequestMapping("/voter")
public class VoteController {

    @Autowired
    private BallotService ballotService;
    Ballot ballot = null;

    @GetMapping("/connect")
    public String showVotePage(Model model) {
        model.addAttribute("privateKey", "");
        model.addAttribute("contractAddress", "");
        model.addAttribute("gasPrice", "20000000000");
        model.addAttribute("gasLimit", "6721975");
        return "vote";
    }

    @GetMapping("/voting")
    public String showVotingPage(Model model) throws Exception {
        long count = 0;
        ArrayList<String> candidates = new ArrayList<>();
        int numberProposals = ballotService.getDeployedBallot().numberProposals().send().intValueExact();
        while (count < numberProposals) {
            try {
                String candidate = (new String(ballotService.getDeployedBallot().proposals(BigInteger.valueOf(count++)).send().getValue1())).trim();
                candidates.add(candidate);
            } catch (Exception ex) {
                Logger.getLogger(VoteController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        model.addAttribute("candidates", candidates);
        return "voting-page";
    }

    @PostMapping("/connect")
    public String handleVote(@RequestParam String action,
                             @RequestParam String privateKey,
                             @RequestParam String contractAddress,
                             @RequestParam String gasPrice,
                             @RequestParam String gasLimit,
                             Model model) {

        Credentials credentials = Credentials.create(privateKey);
        Web3j web3j = Web3j.build(new HttpService());
        ballot = Ballot.load(contractAddress, web3j, credentials, new BigInteger(gasPrice), new BigInteger(gasLimit));
        System.out.println("here");
        try {
            if (!ballot.inProgress().send()) {
                JOptionPane.showMessageDialog(null, "Election has ended.", "Task failed", JOptionPane.ERROR_MESSAGE);
                return "error";
            } else {
                if (!checkVoted(credentials.getAddress())) {
                    if(action.equals("vote")){
                        System.out.println("hehe");
                        return "redirect:/voter/voting";
                    }
                    if(action.equals("delegate")){
                        return "redirect:/voter/delegate";
                    }
                } else {
                    System.out.println("Already voted / delegated");
                    return "error";
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(VoteController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Election ended");
        }
        model.addAttribute("privateKey", privateKey);
        model.addAttribute("contractAddress", contractAddress);
        model.addAttribute("gasPrice", gasPrice);
        model.addAttribute("gasLimit", gasLimit);
        return "voting-page";
    }

    private boolean checkVoted(String addr) {
        try {
            if (ballot.voters(addr).send().getValue2()) {
                return true;
            }
        } catch (Exception ex) {
            Logger.getLogger(VoteController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    @PostMapping("/vote")
    public String handleVote(@RequestParam("proposalIndex") int proposalIndex, Model model) {
        System.out.println("Voted for proposal index: " + proposalIndex);

        try {
            TransactionReceipt tr = ballot.vote(BigInteger.valueOf(proposalIndex)).send();
            if (tr.isStatusOK()) {
                System.out.println("Vote successful: " + tr.isStatusOK());
            } else {
                System.out.println("Vote unsuccessful");
            }
        } catch (Exception ex) {
            System.out.println("Vote unsuccessful because");
        } finally {
            return "vote";
        }
    }

    @GetMapping("/delegate")
    public String showDelegatePage(){
        System.out.println("was here");
        return "delegation-page";
    }

    @PostMapping("/delegation")
    public String handleDelegate(@RequestParam("delegationAddress") String delegationAddress, Model model){
        System.out.println("try there");
        System.out.println(delegationAddress);
        try {
            TransactionReceipt tr = ballotService.getDeployedBallot().delegate(delegationAddress).send();
            if(tr.isStatusOK()) {
                System.out.println("Delegation successful: " + tr.isStatusOK());
            } else {
                System.out.println("Delegation successful: " + tr.isStatusOK());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Delegation unsuccessful.");
        } finally {
            return "vote";
        }
    }
}

