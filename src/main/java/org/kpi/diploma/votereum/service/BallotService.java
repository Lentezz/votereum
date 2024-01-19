package org.kpi.diploma.votereum.service;

import org.kpi.diploma.votereum.entity.Ballot;
import org.springframework.stereotype.Service;

@Service
public class BallotService {
    private Ballot deployedBallot;
    public void setDeployedBallot(Ballot deployedBallot) {
        this.deployedBallot = deployedBallot;
    }
    public Ballot getDeployedBallot() {
        return deployedBallot;
    }
}
