package com.example.to_doapp.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.to_doapp.R
import com.example.to_doapp.databinding.FragmentSignUpBinding
import com.google.firebase.auth.FirebaseAuth
class SignUpFragment : Fragment() {
    private lateinit var navController: NavController
    private lateinit var mAuth: FirebaseAuth
    private lateinit var binding: FragmentSignUpBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view)
        binding.textViewSignIn.setOnClickListener {
            navController.navigate(R.id.action_signUpFragment_to_signInFragment)
        }
        binding.nextBtn.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val pass = binding.passEt.text.toString()
            val verifyPass = binding.verifyPassEt.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty() && verifyPass.isNotEmpty()) {
                if (pass == verifyPass) {
                    checkEmailExistsOrNot(email, pass)
                } else {
                    Toast.makeText(context, "Passwords are not the same", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Empty fields are not allowed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun checkEmailExistsOrNot(email: String, pass: String) {
        mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val isNewUser = task.result?.signInMethods?.isEmpty() == true

                if (isNewUser) {
                    registerUser(email, pass)
                } else {
                    Toast.makeText(context, "Email is already registered", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, task.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun registerUser(email: String, pass: String) {
        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
            if (it.isSuccessful)
                navController.navigate(R.id.action_signUpFragment_to_homeFragment)
            else
                Toast.makeText(context, it.exception.toString(), Toast.LENGTH_SHORT).show()
        }
    }
    private fun init(view: View) {
        navController = Navigation.findNavController(view)
        mAuth = FirebaseAuth.getInstance()
    }
}
