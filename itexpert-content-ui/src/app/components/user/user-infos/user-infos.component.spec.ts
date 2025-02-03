import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserInfosComponent } from './user-infos.component';

describe('UserInfosComponent', () => {
  let component: UserInfosComponent;
  let fixture: ComponentFixture<UserInfosComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [UserInfosComponent]
    });
    fixture = TestBed.createComponent(UserInfosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
