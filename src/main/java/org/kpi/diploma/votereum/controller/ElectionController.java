package org.kpi.diploma.votereum.controller;

import org.kpi.diploma.votereum.service.BallotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.web3j.tuples.generated.Tuple2;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Controller
@RequestMapping("/manage-election")
public class ElectionController {

    @Autowired
    private BallotService ballotService;

    @GetMapping("/main")
    public String manageElection(Model model) {
        model.addAttribute("contractAddress", ballotService.getDeployedBallot().getContractAddress());
        System.out.println(ballotService.getDeployedBallot().getContractAddress());
        return "manage";
    }

    @PostMapping("/authorize-voter")
    public String authorizeVoter(@RequestParam("voterId") String voterId) {
        if(ballotService.getDeployedBallot() != null){
            try {
                ballotService.getDeployedBallot().giveRightToVote(voterId).send();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.out.println(voterId);
        }
        return "redirect:/manage-election/main";
    }

    @GetMapping("/end-election")
    public String endElection(Model model) {
        try {
            ballotService.getDeployedBallot().close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "redirect:/manage-election/results";
    }

    @GetMapping("/results")
    public String results(Model model) {
        try {
            if (ballotService.getDeployedBallot() != null) {
                ballotService.getDeployedBallot().close();
                byte[] winner = ballotService.getDeployedBallot().winnerName().send();
                String winnerName = new String(winner);
                HashMap<String, Integer> resultsMap = new HashMap<>();
                for (long i = 0; i < ballotService.getDeployedBallot().numberProposals().send().intValue(); i++) {
                    Tuple2<byte[], BigInteger> proposal = ballotService.getDeployedBallot().proposals(BigInteger.valueOf(i)).send();
                    String proposalName = new String(proposal.getValue1());
                    Integer votes = proposal.getValue2().intValue();
                    resultsMap.put(proposalName, votes);
                }
                List<java.util.Map.Entry<String, Integer>> sortedResults = new ArrayList<>(resultsMap.entrySet());
                sortedResults.sort(java.util.Map.Entry.<String, Integer>comparingByValue().reversed());
                model.addAttribute("winner", winnerName);
                model.addAttribute("resultsList", sortedResults);
            } else {
                System.out.println("FAIL");
                model.addAttribute("error", "Failed to fetch ballot results.");
            }
        } catch (Exception ex) {
            Logger.getLogger(ElectionController.class.getName()).log(Level.SEVERE, null, ex);
            model.addAttribute("error", "An error occurred while fetching ballot results.");
        }
        return "results";
    }
}
