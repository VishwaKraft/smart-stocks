import { Component, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { EventTrackingService } from '../../../services/event-tracking.service';

@Component({
  selector: 'app-contact-us',
  templateUrl: './contact-us.component.html',
  styleUrls: ['./contact-us.component.css']
})
export class ContactUsComponent implements OnInit {

  firstName: string = '';
  lastName: string = '';
  email: string = '';
  message: string = '';

  constructor(
    private toastr: ToastrService,
    private eventTracker: EventTrackingService
  ) { }

  ngOnInit(): void {
  }

  onSubmit() {
    this.eventTracker.trackEvent('CONTACT_US_SUBMIT', {
      action: 'User submitted contact form',
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
      message: this.message
    });

    this.toastr.success("We will find You !!", "", {
      closeButton: true,
      "positionClass": "toast-bottom-right",
    });

    // Reset form
    this.firstName = '';
    this.lastName = '';
    this.email = '';
    this.message = '';
  }
}
