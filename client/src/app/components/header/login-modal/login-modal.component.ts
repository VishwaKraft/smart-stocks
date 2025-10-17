import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { UserService } from 'src/app/services/user.service';
import { NGXLogger } from 'ngx-logger';

@Component({
  selector: 'app-login-modal',
  templateUrl: './login-modal.component.html',
  styleUrls: ['./login-modal.component.css'],
})
export class LoginModalComponent implements OnInit {
  hide: boolean = true;

  LoginForm: FormGroup = new FormGroup({});

  constructor(
    private _fb: FormBuilder,
    public dialogRef: MatDialogRef<LoginModalComponent>,
    private userService: UserService,
    private toastr: ToastrService,
    private logger: NGXLogger,
  ) {}

  ngOnInit(): void {
    this.LoginForm = this._fb.group({
      email: [null, [Validators.required]],
      password: [null, [Validators.required]],
    });
  }

  onSubmit() {
    this.logger.info('Login form submitted for user:', this.LoginForm.value.email);
    this.logger.debug('Full form payload (includes password):', this.LoginForm.value);
    this.userService.login(this.LoginForm.value).subscribe(
      (data) => {
        localStorage.setItem('token', data.data.token);
        this.logger.info('Login successful! Token saved.');
        this.toastr.success('Login Successfull!', '', {
          closeButton: true,
          positionClass: 'toast-bottom-right',
        });
        this.userService.isLoginSubject.next(true);
        this.dialogRef.close();
      },
      (error) => {
        this.logger.error('Login failed due to API error:', error);
        this.toastr.error(error.error.errors.error + '', '', {
          closeButton: true,
          positionClass: 'toast-bottom-right',
        });
      },
    );
  }
}
