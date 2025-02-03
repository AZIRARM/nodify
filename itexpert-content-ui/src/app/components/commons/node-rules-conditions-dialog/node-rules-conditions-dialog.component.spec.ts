import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NodeRulesConditionsDialogComponent } from './node-rules-conditions-dialog.component';

describe('NodeRulesConditionsDialogComponent', () => {
  let component: NodeRulesConditionsDialogComponent;
  let fixture: ComponentFixture<NodeRulesConditionsDialogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [NodeRulesConditionsDialogComponent]
    });
    fixture = TestBed.createComponent(NodeRulesConditionsDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
