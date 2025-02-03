import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContentChartsComponent } from './content-charts.component';

describe('ContentChartsComponent', () => {
  let component: ContentChartsComponent;
  let fixture: ComponentFixture<ContentChartsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ContentChartsComponent]
    });
    fixture = TestBed.createComponent(ContentChartsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
