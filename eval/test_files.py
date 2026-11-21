from run import load, REQUIRED_TASK, REQUIRED_ATTACK

def test_all_eval_files_are_valid():
    assert load("tasks", REQUIRED_TASK)
    assert load("attacks", REQUIRED_ATTACK)
