package model;

import core.GConfigs;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.MyUtils;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.Ranker;
import weka.attributeSelection.ReliefFAttributeEval;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.PLSFilter;
import weka.filters.unsupervised.attribute.Discretize;
import weka.filters.unsupervised.attribute.InterquartileRange;
import weka.filters.unsupervised.instance.RemoveMisclassified;

public class Filters {

  protected static Instances prepareInstances(Instances rawData, int i, String typePath) throws Exception {
    Instances r = removeUnwantedClassAtt(new Instances(rawData), i, typePath);
    //data_with_one_class=interquartileRange(data_with_one_class);
    r.deleteWithMissingClass();
    ReliefFAttributeEval relf = new ReliefFAttributeEval();
    relf.setOptions(new String[]{"-M", "50"});
    r = Filters.selectAttributes(r, relf);
    //r = Filters.filterByClassifier(r);
    //r = interquartileRange(r);

    r = plsFilter(r);
    return r;
  }

  // Only one class attribute to be predicted at once, so the other class attributes will be removed        
  private static Instances removeUnwantedClassAtt(Instances trainInstances, int classIndex, String typePath) {
    int target = GConfigs.getClassCount(typePath);
    int att_count = trainInstances.numAttributes();
    int training_count = att_count - target;
    trainInstances.setClassIndex(training_count + classIndex);
    for (int i = 0; i < target; i++) {
      int tempind = training_count + i;
      if (trainInstances.classIndex() != tempind) {
        trainInstances.deleteAttributeAt(tempind);
        i--;
        target--;
      }
    }
    return trainInstances;
  }

  private static Instances plsFilter(Instances instances) throws Exception {
    PLSFilter f = new PLSFilter();
    f.setInputFormat(instances);
    f.setOptions(new String[]{"-C", "20", "-M", "-A", "SIMPLS", "-P", "center"});
    return Filter.useFilter(instances, f);
  }

  public static Instances interquartileRange(Instances instances) throws Exception {
    InterquartileRange f = new InterquartileRange();
    f.setAttributeIndices("first-last");
    f.setInputFormat(instances);
    Instances r = Filter.useFilter(instances, f);
    r.deleteAttributeAt(r.numAttributes() - 1);
    r.deleteAttributeAt(r.numAttributes() - 1);
    return r;
  }

  public static Instances discretizeTrainingAtt(Instances instances) throws Exception {
    Discretize disc = new Discretize();
    disc.setAttributeIndices("first-last");
    disc.setIgnoreClass(false);
    disc.setInputFormat(instances);
    return Filter.useFilter(instances, disc);
  }

  public static Instances discretizeClassAtt(Instances instances) throws Exception {
    Discretize disc = new Discretize();
    disc.setAttributeIndices("last");
    disc.setIgnoreClass(true);
    disc.setInputFormat(instances);
    return Filter.useFilter(instances, disc);
  }

  public static Instances filterByClassifier(Instances data) throws Exception {
    RemoveMisclassified filter_by_classifier = new RemoveMisclassified();
    filter_by_classifier.setOptions(new String[]{"-W", "weka.classifiers.trees.RandomForest -I 100"});
    filter_by_classifier.setInputFormat(data);
    return Filter.useFilter(data, filter_by_classifier);
  }

  public static Instances selectAttributes(Instances originalData, ASEvaluation evaluator) throws Exception {
    if (!evaluator.getCapabilities().handles(Capabilities.Capability.NUMERIC_CLASS)) {
      originalData = Filters.discretizeClassAtt(originalData);
    }
    AttributeSelection selection = new AttributeSelection();
    Ranker ranker = new Ranker();
    ranker.setOptions(new String[]{"-N", "20"});
    selection.setEvaluator(evaluator);
    selection.setSearch(ranker);
    selection.SelectAttributes(originalData);
    return selection.reduceDimensionality(originalData);
  }

  protected static Instances selectUnfavoredAttributes(String tittle,
          Instances originalData, WekaModelBenchamrker wekaModelBenchamrker)
          throws Exception {
    AttributeSelection selection = new AttributeSelection();
    Ranker ranker = new Ranker();
    ranker.setOptions(new String[]{"-N", "20"});
    ReliefFAttributeEval eval = new ReliefFAttributeEval();
    eval.setOptions(new String[]{"-M", "50"});
    selection.setEvaluator(eval);
    selection.setSearch(ranker);
    selection.SelectAttributes(originalData);
    int[] selected_indexes = selection.selectedAttributes();
    Arrays.sort(selected_indexes);
    //printSelectedAtts("primary_" + tittle, selected_indexes, originalData);
    for (int i = selected_indexes.length - 2; i >= 0; i--) {
      originalData.deleteAttributeAt(selected_indexes[i]);
    }
    selection.SelectAttributes(originalData);
    selected_indexes = selection.selectedAttributes();
    Arrays.sort(selected_indexes);
    //printSelectedAtts("secondary_" + tittle, selected_indexes, originalData);
    return selection.reduceDimensionality(originalData);
  }

  protected static Instances weightDataByDate(Instances data) {
    LocalDate tempdate;
    Instance ins;
    for (int i = 0; i < data.numInstances(); i++) {
      ins = data.instance(i);
      tempdate = MyUtils.parseToISO(ins.stringValue(0));
      int yeardiff = LocalDate.now().getYear() - tempdate.getYear();
      double weight = -0.2 * yeardiff + 1;
      if (weight < 0) {
        weight = 0;
      }
      ins.setWeight(weight);
    }
    return data;
  }

  private static void printSelectedAtts(String tittle,
          int[] selected, Instances header) {
    File file = new File(GConfigs.REPORT_PATH + "out\\Selected_" + tittle + ".csv");
    try (PrintWriter writer = new PrintWriter(new BufferedWriter(
            new FileWriter(file, true)))) {
      writer.println("----");
      for (int index : selected) {
        writer.println(header.attribute(index).name());

      }
    } catch (Exception ex) {
      Logger.getLogger(WekaModelBenchamrker.class
              .getName()).log(Level.SEVERE, null, ex);
    }
  }
}
