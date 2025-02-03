import { UserNamePipe } from './user-name.pipe';

describe('UserNamePipe', () => {
  it('create an instance', () => {
    const pipe = new UserNamePipe();
    expect(pipe).toBeTruthy();
  });
});
