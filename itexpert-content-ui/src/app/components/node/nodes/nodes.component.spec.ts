import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NodesComponent } from './nodes.component';

describe('NodesComponent', () => {
  let component: NodesComponent;
  let fixture: ComponentFixture<NodesComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [NodesComponent]
    });
    fixture = TestBed.createComponent(NodesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
