import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { UserService } from 'src/app/services/user.service';
import { NGXLogger } from 'ngx-logger';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
})
export class ProfileComponent implements OnInit {
  profileForm: FormGroup = new FormGroup({});

  constructor(
    private _fb: FormBuilder,
    private userService: UserService,
    private router: Router,
    private toastr: ToastrService,
    private logger: NGXLogger,
  ) {
    this.profileForm = this._fb.group({
      firstName: [null, [Validators.required]],
      lastName: [null, [Validators.required]],
      phoneNo: [null, [Validators.required]],
      dob: [null, [Validators.required]],
      gender: [null, [Validators.required]],
      email: [null, [Validators.required]],
    });
    this.userService.getUser().subscribe(
      (data) => {
        this.logger.debug('User profile data loaded successfully.', data);
        this.profileForm.patchValue(data.data);
      },
      (err) => {
        this.logger.error('Failed to fetch user profile data:', err);
      },
    );
  }

  ngOnInit(): void {
    window.scrollTo(0, 0);
  }

  onSubmit() {
    this.logger.info('Profile update submitted for user.');
    this.userService.updateUser(this.profileForm.value).subscribe(
      (data) => {
        this.logger.info('User profile updated successfully.');
        this.toastr.success('Update Successfull!', '', {
          closeButton: true,
          positionClass: 'toast-bottom-right',
        });
      },
      (error) => {
        this.logger.error('Profile update failed with API error:', error);
        this.toastr.error(error.error.errors.error + '', '', {
          closeButton: true,
          positionClass: 'toast-bottom-right',
        });
      },
    );
    this.router.navigateByUrl('/user/sandbox/dashboard');
  }

  handleBack() {
    this.router.navigateByUrl('/user/sandbox/dashboard');
  }
}
