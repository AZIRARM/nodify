export class FeedbackCharts {
  contentName: string;
  contentCode: string;
  charts: Chart[];
  verified: Chart[];
  notVerified: Chart[]
}

class Chart {
  public name: string;
  public value: string;
}
