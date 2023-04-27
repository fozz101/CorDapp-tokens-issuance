package tn.talan.com.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;
import tn.talan.com.states.TokenState;

/**
 * @author fozz101
 */
public class TokenContract implements Contract {
    public static String ID = "tn.talan.com.contracts.TokenContract";



    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        //no input state
        if(tx.getInputStates().size()!=0){
            throw new IllegalArgumentException("Token Contract requires 0 input in the transaction!");
        }
        // 1 output state
        if(tx.getOutputStates().size()!=1){
            throw new IllegalArgumentException("Token Contract requires 1 output in the transaction!");
        }
        // 1 command
        if(tx.getCommands().size()!=1) {
            throw new IllegalArgumentException("Token Contract requires 1 command in the transaction!");
        }
        // transaction output type of TokenState
        if(!(tx.getOutput(0) instanceof TokenState) ){
            throw new IllegalArgumentException("Token Contract requires output of type TokenState!");
        }
        // positive transaction output amount
        TokenState tokenState =  ((TokenState) tx.getOutput(0));
        if(tokenState.getAmount()<0){
            throw new IllegalArgumentException("Token Contract requires positive transaction output amount");
        }

        //transaction command must be Issue Command
        if(!(tx.getCommand(0).getValue() instanceof  TokenContract.Commands.Issue)){
            throw new IllegalArgumentException("Token Contract requires Issue transaction command");
        }
        //issuer is a required signer
        if(!(tx.getCommand(0).getSigners().contains(tokenState.getIssuer().getOwningKey()))){
            throw new IllegalArgumentException("Token Contract requires issuer is a signer!");
        }

        // notary shouldn't be an owner or issuer
        if(tokenState.getIssuer().equals(tx.getNotary())){
            throw new IllegalArgumentException("Token Contract requires Notary is not issuer");
        }
        if(tokenState.getOwner().equals(tx.getNotary())){
            throw new IllegalArgumentException("Token Contract requires Notary is not owner");
        }

    }

    public interface Commands extends CommandData{
        class Issue implements Commands { }
    }
}
