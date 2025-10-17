import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { UserService } from 'src/app/services/user.service';
import { NGXLogger } from 'ngx-logger';

@Component({
  selector: 'app-signup-modal',
  templateUrl: './signup-modal.component.html',
  styleUrls: ['./signup-modal.component.css'],
})
export class SignupModalComponent implements OnInit {
  signupForm: FormGroup = new FormGroup({});

  constructor(
    private _fb: FormBuilder,
    public dialogRef: MatDialogRef<SignupModalComponent>,
    private userService: UserService,
    private toastr: ToastrService,
    private logger: NGXLogger,
  ) {}

  ngOnInit(): void {
    this.signupForm = this._fb.group({
      firstName: [null, [Validators.required]],
      lastName: [null, [Validators.required]],
      phoneNo: [null, [Validators.required]],
      dob: [null, [Validators.required]],
      gender: [null, [Validators.required]],
      email: [null, [Validators.required]],
      password: [null, [Validators.required]],
    });
  }

  onSubmit() {
    this.logger.info('Signup form submitted for user:', this.signupForm.value.email);
    this.logger.debug('Full signup payload submitted (includes PII):', this.signupForm.value);
    this.userService.signup(this.signupForm.value).subscribe(
      (data) => {
        this.logger.info('Signup successful. Closing modal.');
        this.toastr.success('Signup Successfull!', '', {
          closeButton: true,
          positionClass: 'toast-bottom-right',
        });
        this.dialogRef.close();
      },
      (error) => {
        this.logger.error('Signup failed due to API error:', error);
        this.toastr.error(error.error.errors.error, '', {
          closeButton: true,
          positionClass: 'toast-bottom-right',
        });
      },
    );
  }
}
