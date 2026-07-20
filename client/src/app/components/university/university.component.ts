import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { UserService } from 'src/app/services/user.service';
import { LoginModalComponent } from '../header/login-modal/login-modal.component';

@Component({
  selector: 'app-university',
  templateUrl: './university.component.html',
  styleUrls: ['./university.component.css']
})
export class UniversityComponent implements OnInit {

  isLogin: boolean

  constructor(
    private userService: UserService, 
    private toastr: ToastrService, 
    private router: Router,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.userService.isLoggedIn().subscribe((value) => {
      this.isLogin = value
    })
    this.isLogin = (localStorage.getItem('token') != null)
  }

  redirect() {
    if (this.isLogin === false) {
      this.toastr.info("Please login to access the Sandbox", "", {
        closeButton: true,
        positionClass: "toast-bottom-right",
      });
      const dialogConfig = new MatDialogConfig();
      dialogConfig.autoFocus = true;
      this.dialog.open(LoginModalComponent, dialogConfig);
    } else {
      this.router.navigate(['/user/sandbox/dashboard']);
    }
  }

}
