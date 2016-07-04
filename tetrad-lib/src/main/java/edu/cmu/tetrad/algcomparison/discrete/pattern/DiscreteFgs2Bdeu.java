package edu.cmu.tetrad.algcomparison.discrete.pattern;

import edu.cmu.tetrad.algcomparison.Algorithm;
import edu.cmu.tetrad.algcomparison.DataType;
import edu.cmu.tetrad.algcomparison.Parameters;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.*;

/**
 * Created by jdramsey on 6/4/16.
 */
public class DiscreteFgs2Bdeu implements Algorithm {
    public Graph search(DataSet dataSet, Parameters parameters) {
        BDeuScore score = new BDeuScore(dataSet);
        score.setSamplePrior(parameters.getInt("samplePrior"));
        score.setSamplePrior(parameters.getInt("structurePrior"));
        Fgs2 fgs = new Fgs2(score);
//        fgs.setDepth(parameters.get("fgsDepth"));
        return fgs.search();
    }

    public Graph getComparisonGraph(Graph dag) {
        return new DagToPag(dag).convert();
    }

    public String getDescription() {
        return "FGS2 using the BDeu score";
    }

    @Override
    public DataType getDataType() {
        return DataType.Discrete;
    }
}
