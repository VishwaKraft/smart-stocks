import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, FormControl } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { UserService } from 'src/app/services/user.service';

@Component({
  selector: 'app-login-modal',
  templateUrl: './login-modal.component.html',
  styleUrls: ['./login-modal.component.css']
})
export class LoginModalComponent implements OnInit {

  hide: boolean = true;

  LoginForm: FormGroup = new FormGroup({});

  constructor(private _fb: FormBuilder, public dialogRef: MatDialogRef<LoginModalComponent>, private userService: UserService, private toastr: ToastrService) { }

  ngOnInit(): void {
    this.LoginForm = this._fb.group({
      email: [null, [Validators.required]],
      password: [null, [Validators.required]],
    });
  }

  onSubmit() {
    console.log(this.LoginForm.value);
    this.userService.login(this.LoginForm.value).subscribe(
      data => {
        localStorage.setItem("token", data.data.token)
        this.toastr.success("Login Successfull!", "", {
          closeButton: true,
          "positionClass": "toast-bottom-right",
        })
        this.userService.isLoginSubject.next(true);
        this.dialogRef.close();
      },
      error => {
        console.log('Something went wrong !')
        this.toastr.error(error.error.errors.error + "", "", {
          closeButton: true,
          "positionClass": "toast-bottom-right",
        })
      }
    );

  }

  signInWithGoogle() {
    this.toastr.info("Google Sign-In is currently being set up by the admin.", "Coming Soon", {
      closeButton: true,
      positionClass: "toast-bottom-right",
    });
  }

}
