package Translate;
import Temp.Temp;
import Temp.Label;

//some variation in the testcases. not sure why yet

class IfThenElseExp extends Exp {
  Exp cond, a, b;
  Label t = new Label();
  Label f = new Label();
  Label join = new Label();

  IfThenElseExp(Exp cc, Exp aa, Exp bb) {
    cond = cc; 
    a = aa; 
    b = bb;
  }

Tree.Stm unCx(Label tt, Label ff) {
  Label bTrueLabel = new Label();
  Label joinLabel = new Label();
  Tree.Stm aStm = a.unCx(tt, ff);
  Tree.Stm bStm = b.unCx(tt, ff);
  if (b == null) {
    return aStm == null ? null : new Tree.SEQ(aStm, new Tree.LABEL(joinLabel));
  } else {
    return aStm == null ? new Tree.SEQ(bStm, new Tree.LABEL(joinLabel)) :
      new Tree.SEQ(aStm, new Tree.SEQ(new Tree.LABEL(bTrueLabel),
        new Tree.SEQ(bStm, new Tree.LABEL(joinLabel))));
  }
}


  Tree.Exp unEx() {
    Temp result = new Temp();
    Tree.Stm condStm = cond.unCx(t, f);
    Tree.Stm aStm = new Tree.MOVE(new Tree.TEMP(result), a.unEx());
    Tree.Stm bStm = new Tree.MOVE(new Tree.TEMP(result), b.unEx());
    Tree.Stm trueStm = new Tree.SEQ(new Tree.LABEL(t), aStm);
    Tree.Stm falseStm = new Tree.SEQ(new Tree.LABEL(f), bStm);
    Tree.Stm joinStm = new Tree.LABEL(join);
    return new Tree.ESEQ(new Tree.SEQ(condStm, new Tree.SEQ(trueStm, new Tree.SEQ(joinStm, falseStm))),
                         new Tree.TEMP(result));
  }

  Tree.Stm unNx() {
    Tree.Stm condStm = cond.unCx(t, f);
    Tree.Stm trueStm = a.unNx();
    Tree.Stm falseStm = b.unNx();
    Tree.Stm joinStm = new Tree.JUMP(join);
    return new Tree.SEQ(condStm, new Tree.SEQ(trueStm, new Tree.SEQ(falseStm, new Tree.SEQ(joinStm, new Tree.LABEL(join)))));
}
  

}
